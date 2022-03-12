<?php
namespace App\Controller;


use App\Form\Type\RatingType;
use App\Entity\{Archive, Author, Chapter, Rating, Story, Version};
use Doctrine\Persistence\{ManagerRegistry, ObjectManager};
use Psr\Log\LoggerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\Form\Extension\Core\Type\{CheckboxType, SubmitType, TextareaType};
use Symfony\Component\HttpFoundation\{Response, Request};
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Authentication\AuthenticationUtils;


class AuthController extends AbstractController {
    private ManagerRegistry $doctrine;
    private LoggerInterface $logger;

    public function __construct(ManagerRegistry $doctrine, LoggerInterface $logger) {
        $this->doctrine = $doctrine;
        $this->logger = $logger;
    }


    #[Route('/login', name: 'auth_login')]
    public function login(AuthenticationUtils $authUtils): Response {
        $lastUsername = $authUtils->getLastUsername();
        $error = $authUtils->getLastAuthenticationError();

        return $this->render('web/auth/login.html.twig', [
            'last_username' => $lastUsername,
            'error' => $error
        ]);
    }

    #[Route('/logout', name: 'auth_logout')]
    public function logout(): Response {
        throw new \RuntimeException();
    }
}
