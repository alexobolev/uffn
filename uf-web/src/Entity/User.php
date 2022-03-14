<?php
namespace App\Entity;

use App\Repository\UserRepository;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Security\Core\User\PasswordAuthenticatedUserInterface;
use Symfony\Component\Security\Core\User\UserInterface;


#[ORM\Entity(repositoryClass: UserRepository::class)]
#[ORM\Table(name: 'users')]
class User implements UserInterface, PasswordAuthenticatedUserInterface {

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
        length: 25
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
        type: 'boolean'
    )]
    private ?bool $isAdmin;

    #[ORM\OneToMany(
        mappedBy: 'owner',
        targetEntity: UploadSession::class,
        orphanRemoval: true
    )]
    private $uploadSessions;


    public function __construct() {
        $this->uploadSessions = new ArrayCollection();
    }


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

    /**
     * @return Collection<int, UploadSession>
     */
    public function getUploadSessions(): Collection {
        return $this->uploadSessions;
    }

    public function addUploadSession(UploadSession $uploadSession): self {
        if (!$this->uploadSessions->contains($uploadSession)) {
            $this->uploadSessions[] = $uploadSession;
            $uploadSession->setOwner($this);
        }
        return $this;
    }

    public function removeUploadSession(UploadSession $uploadSession): self {
        if ($this->uploadSessions->removeElement($uploadSession)) {
            // set the owning side to null (unless already changed)
            if ($uploadSession->getOwner() === $this) {
                $uploadSession->setOwner(null);
            }
        }
        return $this;
    }

    /// Authentication-related stuff.
    /// ========================================

    public function getUserIdentifier(): string {
        return (string)$this->email;
    }

    public function getRoles(): array {
        $roles = [ 'ROLE_USER' ];
        if ($this->isAdmin) {
            $roles[] = 'ROLE_ADMIN';
        }
        return array_unique($roles);
    }

    public function getPassword(): string {
        return (string)$this->password;
    }

    public function setPassword(?string $password): self {
        $this->password = $password;
        return $this;
    }

    public function eraseCredentials() {
        // ...
    }
}
