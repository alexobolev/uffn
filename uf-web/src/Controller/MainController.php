<?php
namespace App\Controller;


use App\Form\Type\RatingType;
use App\Entity\{Archive, Author, Chapter, Rating, Story, User, Version};
use Doctrine\Persistence\{ManagerRegistry, ObjectManager};
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\Form\Extension\Core\Type\{CheckboxType, SubmitType, TextareaType};
use Symfony\Component\HttpFoundation\{Response, Request};
use Symfony\Component\Routing\Annotation\Route;


class MainController extends AbstractController {

    public function __construct() { }

    #[Route('/', name: 'main_index')]
    public function index(): Response {
        return $this->redirectToRoute('profile');
    }
}
