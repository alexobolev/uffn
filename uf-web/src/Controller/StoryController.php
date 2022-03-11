<?php
namespace App\Controller;


use App\Repository\ChapterRepository;
use App\Entity\{Archive, Author, Chapter, Rating, Story, Version};
use Doctrine\Persistence\{ManagerRegistry, ObjectManager};
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;


class StoryController extends AbstractController {
    private ManagerRegistry $doctrine;

    public function __construct(ManagerRegistry $doctrine) {
        $this->doctrine = $doctrine;
    }


    #[Route(
        '/story/{id}',
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
        '/story/{id}/versions',
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
}
