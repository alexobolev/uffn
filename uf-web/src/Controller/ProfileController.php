<?php
namespace App\Controller;


use App\Form\Model\PasswordChangeFormModel;
use App\Form\Type\PasswordChangeFormType;
use App\Form\Type\RatingType;
use App\Entity\{Archive, Author, Chapter, Rating, Story, User, Version};
use Doctrine\Persistence\{ManagerRegistry, ObjectManager};
use Psr\Log\LoggerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\Form\Extension\Core\Type\{CheckboxType,
    EmailType,
    PasswordType,
    RepeatedType,
    SubmitType,
    TextareaType,
    TextType};
use Symfony\Component\HttpFoundation\{Response, Request};
use Symfony\Component\PasswordHasher\Hasher\UserPasswordHasherInterface;
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


    #[Route('/profile/settings', name: 'profile_settings')]
    public function settings(Request $request, LoggerInterface $logger, UserPasswordHasherInterface $hasher): Response {
        /** @var User $user */
        $user = $this->getUser();

        $pwdFormModel  = PasswordChangeFormModel::fromEntity($user);
        $pwdForm = $this->createForm(PasswordChangeFormType::class, $pwdFormModel);

        $pwdForm->handleRequest($request);
        if ($pwdForm->isSubmitted() && $pwdForm->isValid()) {
            // current password has been checked by the validation layer

            $pwdFormModel = $pwdForm->getData();
            $changedPassword = $hasher->hashPassword($user, $pwdFormModel->new_password);

            $user->setPassword($changedPassword);
            $this->doctrine->getManager()->flush();

            return $this->redirectToRoute('auth_logout');
        }

        return $this->renderForm('web/profile/settings.html.twig', [
            'pwd_form' => $pwdForm
        ]);
    }
}
