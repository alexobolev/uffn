<?php
namespace App\Entity;

use App\Repository\UserRepository;
use Doctrine\ORM\Mapping as ORM;


#[ORM\Entity(repositoryClass: UserRepository::class)]
#[ORM\Table(name: 'users')]
class User {

    #[ORM\Id()]
    #[ORM\GeneratedValue(strategy: 'IDENTITY')]
    #[ORM\Column(
        name: 'id',
        type: "integer"
    )]
    private ?int $id;

    #[ORM\Column(
        name: 'login',
        type: "string",
        length: 255
    )]
    private ?string $login;

    #[ORM\Column(
        name: 'email',
        type: "string",
        length: 255
    )]
    private ?string $email;

    #[ORM\Column(
        name: 'password',
        type: "string",
        length: 255
    )]
    private ?string $password;

    #[ORM\Column(
        name: 'registered_at',
        type: 'datetime'
    )]
    private ?\DateTime $registeredAt;

    #[ORM\Column(
        name: 'login_at',
        type: 'datetime'
    )]
    private ?\DateTime $loginAt;

    #[ORM\Column(
        name: 'is_admin',
        type: 'bool'
    )]
    private ?bool $isAdmin;


    public function getId(): ?int {
        return $this->id;
    }

    public function getLogin(): ?string {
        return $this->login;
    }

    public function setLogin(string $login): self {
        $this->login = $login;
        return $this;
    }

    public function getEmail(): ?string {
        return $this->email;
    }

    public function setEmail(string $email): self {
        $this->email = $email;
        return $this;
    }

    public function getRegisteredAt(): ?\DateTime {
        return $this->registeredAt;
    }

    public function setRegisteredAt(\DateTime $registeredAt): self {
        $this->registeredAt = $registeredAt;
        return $this;
    }

    public function getLoginAt(): ?\DateTime {
        return $this->loginAt;
    }

    public function setLoginAt(?\DateTime $loginAt): self {
        $this->loginAt = $loginAt;
        return $this;
    }

    public function getIsAdmin(): ?bool {
        return $this->isAdmin;
    }

    public function setIsAdmin(bool $isAdmin): self {
        $this->isAdmin = $isAdmin;
        return $this;
    }
}
