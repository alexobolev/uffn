<?php
namespace App\Form\Model;

use App\Entity\User;
use Symfony\Component\Security\Core\User\UserInterface;


class PasswordChangeFormModel {

    public string $login;
    public string $email;
    public string $old_password;
    public string $new_password;

    public function __construct(string $login, string $email) {
        $this->login = $login;
        $this->email = $email;
    }

    public static function fromEntity(?UserInterface $user): self {
        if ($user === null) {
            throw new \RuntimeException (
                'Can\'t create a password change form model from a null User instance'
            );
        }
        return new PasswordChangeFormModel (
            login: $user->getLogin(),
            email: $user->getEmail()
        );
    }
}
