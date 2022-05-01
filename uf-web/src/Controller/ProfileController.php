<?php
namespace App\Controller;


use App\Form\Model\PasswordChangeFormModel;
use App\Form\Type\PasswordChangeFormType;
use App\Repository\UploadSessionRepository;
use App\Repository\VersionRepository;
use Doctrine\ORM\EntityManager;
use App\Entity\{UploadSession, User, Version};
use DeviceDetector\DeviceDetector;
use DeviceDetector\Parser\Client\Browser;
use DeviceDetector\Parser\OperatingSystem;
use Doctrine\Persistence\{ManagerRegistry, ObjectManager};
use Psr\Log\LoggerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\{Response, Request};
use Symfony\Component\PasswordHasher\Hasher\UserPasswordHasherInterface;
use Symfony\Component\Routing\Annotation\Route;


class ProfileController extends AbstractController {
    private ManagerRegistry $doctrine;
    private LoggerInterface $logger;

    private const kFeedMaxPerPage = 25;

    public function __construct(ManagerRegistry $doctrine, LoggerInterface $logger) {
        $this->doctrine = $doctrine;
        $this->logger = $logger;
    }


    #[Route('/profile', name: 'profile')]
    public function index(): Response {
        return $this->redirectToRoute('profile_feed');
    }


    #[Route('/profile/feed', name: 'profile_feed')]
    public function feed(Request $request): Response {

        /** @var VersionRepository $repository */
        $repository = $this->doctrine->getRepository(Version::class);
        /** @var User $user */
        $user = $this->getUser();

        $pageIndex = $request->query->getInt('page', default: 1);
        if ($pageIndex < 1) { $pageIndex = 1; }

        $versionCount = $repository->countLastOfStoryUploaded(owner: $user);
        $versions = $repository->findLastOfStoryUploadedInReverseArchivalOrderLimited (
            owner: $user,
            offset: ($pageIndex - 1) * self::kFeedMaxPerPage,
            limit: self::kFeedMaxPerPage
        );

        return $this->render('web/profile/feed.html.twig', [
            'pagination' => [
                'total' => ceil($versionCount / self::kFeedMaxPerPage),
                'current' => $pageIndex
            ],
            'versions' => $versions
        ]);
    }

    #[Route('/profile/uploads', name: 'profile_uploads')]
    public function uploads(Request $request): Response {

        /** @var UploadSessionRepository $repository */
        $repository = $this->doctrine->getRepository(UploadSession::class);

        /** @var EntityManager $manager */
        $manager = $this->doctrine->getManager();

        /** @var User $user */
        $user = $this->getUser();

        $uploadKey = $request->getSession()->get('uf-upload-key');
        $uploadSession = $repository->findUsableOwnedBy(owner: $user, key: $uploadKey);

        if ($uploadSession === null) {
            // this allows to "recreate" a new session with the old key,
            // even if that supposedly unique key is already in the table
            $repository->removeByKeyOwnedBy(owner: $user, key: $uploadKey);

            $utc = new \DateTimeZone('UTC');
            $currentTimestamp = new \DateTime('now', timezone: $utc);
            $expiryTimestamp = new \DateTime('+15 minutes', timezone: $utc);

            $uploadSession = (new UploadSession())
                ->setOwner($user)
                ->setAuthKey($uploadKey)
                ->setCreatedAt($currentTimestamp)
                ->setExpiresAt($expiryTimestamp)
                ->setUserAgent($request->headers->get('User-Agent'))
                ->setUserAddress($request->getClientIp());

            $manager->persist($uploadSession);
            $manager->flush();
        }

        return $this->render('web/profile/uploads.html.twig', [
            'upload_session' => $uploadSession,
            'local_data' => [
                'sess_key' => $uploadKey,
                'ws_url' => $this->getParameter('app.websocket_url')
            ],
            'debug' => [ 'sess_key' => $uploadKey ]
        ]);
    }

    #[Route('/profile/settings', name: 'profile_settings')]
    public function settings(Request $request, UserPasswordHasherInterface $hasher): Response {

        /** @var User $user */
        $user = $this->getUser();


        // Password change logic:

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


        // Session listing logic:

        $sessions = [];
        $sessionEntities = $user->getUploadSessions()->getValues();

        if (count($sessionEntities) > 0) {
            $currentTime = new \DateTime();

            $sessions = array_map(function (UploadSession $session) use ($currentTime): array {
                return [
                    'key' => $session->getAuthKey(),
                    'expires' => $session->getExpiresAt(),
                    'stale' => $session->getExpiresAt() < $currentTime,
                    'ip' => $session->getUserAddress(),
                    'ua' => $session->getUserAgent() ?? 'UA N/A',
                ];
            }, $sessionEntities);
        }

        return $this->renderForm('web/profile/settings.html.twig', [
            'pwd_form' => $pwdForm,
            'sessions' => $sessions,
        ]);
    }

    #[Route('/profile/settings/kill_all_sessions', name: 'profile_settings_kill_all_sessions')]
    public function settingsKillAllSessions(): Response {
        /** @var UploadSessionRepository $sessionRepo */
        $sessionRepo = $this->doctrine->getRepository(UploadSession::class);
        $sessionRepo->removeAllOwnedBy($this->getUser());
        return $this->redirectToRoute('profile_settings');
    }

    #[Route('/profile/settings/kill_stale_sessions', name: 'profile_settings_kill_stale_sessions')]
    public function settingsKillStaleSessions(): Response {
        /** @var UploadSessionRepository $sessionRepo */
        $sessionRepo = $this->doctrine->getRepository(UploadSession::class);
        $sessionRepo->removeStaleOwnedBy(owner: $this->getUser());
        return $this->redirectToRoute('profile_settings');
    }
}
