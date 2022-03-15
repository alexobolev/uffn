<?php
namespace App\Controller;


use App\Form\Type\DeleteEntityFormType;
use App\Form\Type\RatingType;
use App\Entity\{Archive, Author, Chapter, Rating, Story, Version};
use Doctrine\ORM\EntityManager;
use Doctrine\Persistence\{ManagerRegistry, ObjectManager};
use Psr\Log\LoggerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\Form\Extension\Core\Type\{CheckboxType, SubmitType, TextareaType};
use Symfony\Component\HttpFoundation\{Response, Request};
use Symfony\Component\Routing\Annotation\Route;


class StoryController extends AbstractController {
    private ManagerRegistry $doctrine;
    private LoggerInterface $logger;

    public function __construct(ManagerRegistry $doctrine, LoggerInterface $logger) {
        $this->doctrine = $doctrine;
        $this->logger = $logger;
    }


    #[Route(
        '/stories/{id}',
        name: 'story',
        requirements: ['id' => '\d+']
    )]
    public function index(int $id): Response {
        $story = $this->doctrine
            ->getRepository(Story::class)
            ->find($id);

        if (!$story) {
            throw $this->createNotFoundException (
                'no story found for id = ' . $id
            );
        }

        $latestVersion = $this->doctrine
            ->getRepository(Version::class)
            ->findMostRecent($story);

        return $this->redirectToRoute('version', ['id' => $latestVersion->getId()]);
    }


    #[Route(
        '/stories/{id}/versions',
        name: 'story_versions',
        requirements: ['id' => '\d+']
    )]
    public function versions(int $id): Response {
        $story = $this->doctrine
            ->getRepository(Story::class)
            ->find($id);

        if (!$story) {
            throw $this->createNotFoundException (
                'no story found for id = ' . $id
            );
        }

        $versions = $this->doctrine
            ->getRepository(Version::class)
            ->findAllSorted($story);

        return $this->render('web/story/versions.html.twig', [
            'story' => [
                '_instance' => $story,
                'id' => $story->getId(),
                'origin' => [
                    'archive' => $story->getOriginArchive()->value,
                    'identifier' => $story->getOriginIdentifier()
                ],
                'info' => [
                    'public' => $story->getIsPublic(),
                    'owner' => $story->getOwner(),
                ],
                'custom' => [
                    'summary' => $story->getSummary(),
                    'rating' => $story->getRating(),
                ]
            ],
            'versions' => $versions,
        ]);
    }


    #[Route(
        '/stories/{id}/edit',
        name: 'story_edit',
        requirements: ['id' => '\d+']
    )]
    public function edit(int $id, Request $request): Response {
        $story = $this->doctrine
            ->getRepository(Story::class)
            ->find($id);

        if (!$story) {
            throw $this->createNotFoundException (
                'no story found for id = ' . $id
            );
        }

        $storyUpdated = false;

        $form = $this->createFormBuilder($story)
            ->add('summary', TextareaType::class, [ 'required' => false ])
            ->add('rating', RatingType::class, [ 'required' => true ])
            ->add('isPublic', CheckboxType::class, [ 'required' => false ])
            ->add('save', SubmitType::class)
            ->getForm();

        $form->handleRequest($request);
        if ($form->isSubmitted() && $form->isValid()) {
            $story = $form->getData();

            $this->logger->debug('Updating story...', [
                'story_id' => $story->getId(),
                'new_rating' => $story->getRating(),
                'new_visibility' => $story->getIsPublic()
            ]);

            $this->doctrine->getManager()->flush();
            $storyUpdated = true;
        }

        return $this->renderForm('web/story/edit.html.twig', [
            'story' => [
                '_instance' => $story,
                'id' => $story->getId(),
                'origin' => [
                    'archive' => $story->getOriginArchive()->value,
                    'identifier' => $story->getOriginIdentifier()
                ]
            ],
            'form' => $form,
            'story_updated' => $storyUpdated
        ]);
    }

    #[Route(
        '/stories/{id}/delete',
        name: 'story_delete',
        requirements: ['id' => '\d+']
    )]
    public function delete(int $id, Request $request): Response {
        $story = $this->doctrine
            ->getRepository(Story::class)
            ->find($id);

        if (!$story) {
            throw $this->createNotFoundException (
                'no story found for id = ' . $id
            );
        }

        $deleteForm = $this->createForm(DeleteEntityFormType::class);
        $deleteForm->handleRequest($request);

        if ($deleteForm->isSubmitted() && $deleteForm->isValid()) {
            /** @var EntityManager $entityManager */
            $entityManager = $this->doctrine->getManager();
            $entityManager->remove($story);
            $entityManager->flush();

            return $this->redirectToRoute('profile');
        }

        return $this->renderForm('web/story/delete.html.twig', [
            'story' => [
                '_instance' => $story,
                'id' => $story->getId(),
                'origin' => [
                    'archive' => $story->getOriginArchive()->value,
                    'identifier' => $story->getOriginIdentifier()
                ]
            ],
            'form' => $deleteForm,
        ]);
    }
}
