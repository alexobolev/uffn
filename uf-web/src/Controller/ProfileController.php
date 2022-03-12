<?php
namespace App\Controller;


use App\Form\Type\RatingType;
use App\Entity\{Archive, Author, Chapter, Rating, Story, User, Version};
use Doctrine\Persistence\{ManagerRegistry, ObjectManager};
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\Form\Extension\Core\Type\{CheckboxType, SubmitType, TextareaType};
use Symfony\Component\HttpFoundation\{Response, Request};
use Symfony\Component\Routing\Annotation\Route;


class ProfileController extends AbstractController {
    private ManagerRegistry $doctrine;

    public function __construct(ManagerRegistry $doctrine) {
        $this->doctrine = $doctrine;
    }


    #[Route('/profile', name: 'profile')]
    public function index(): Response {
        return $this->redirectToRoute('profile_feed');
    }


    #[Route('/profile/feed', name: 'profile_feed')]
    public function feed(): Response {
        $versions = $this->doctrine
            ->getRepository(Version::class)
            ->findVisibleInArchivalOrder($this->getUser());

        return $this->render('web/profile/feed.html.twig', [
            'versions' => $versions
        ]);
    }
}
