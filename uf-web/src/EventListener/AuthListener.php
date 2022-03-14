<?php
namespace App\EventListener;

use Psr\Log\LoggerInterface;
use Symfony\Component\EventDispatcher\EventSubscriberInterface;
use Symfony\Component\HttpFoundation\{Request, Response};
use Symfony\Component\Security\Http\Event\LoginSuccessEvent;


class AuthListener implements EventSubscriberInterface {

    private LoggerInterface $logger;

    public function __construct(LoggerInterface $logger) {
        $this->logger = $logger;
    }

    public function onLoginSuccess(LoginSuccessEvent $event): void {
        $request = $event->getRequest();
        $session = $request->getSession();

        $authBytes = random_bytes(32);
        $authKey = hash(algo: 'sha256', data: bin2hex($authBytes), binary: false);

        $session->set('uf-upload-key', $authKey);
        $this->logger->debug('Listened to LoginSuccessEvent via AuthListener!', ['auth_key' => $authKey]);
    }

    public static function getSubscribedEvents(): array {
        return [
            LoginSuccessEvent::class => 'onLoginSuccess'
        ];
    }
}
