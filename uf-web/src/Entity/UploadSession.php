<?php
namespace App\Entity;

use App\Repository\UploadSessionRepository;
use Doctrine\ORM\Mapping as ORM;


#[ORM\Entity(repositoryClass: UploadSessionRepository::class)]
#[ORM\Table(name: 'upload_sessions')]
class UploadSession {

    #[ORM\Id]
    #[ORM\GeneratedValue(strategy: 'IDENTITY')]
    #[ORM\Column(
        name: 'id',
        type: 'integer'
    )]
    private ?int $id;

    #[ORM\ManyToOne(targetEntity: User::class, inversedBy: 'uploadSessions')]
    #[ORM\JoinColumn(
        name: 'owner_id',
        referencedColumnName: 'id',
        nullable: false
    )]
    private ?User $owner;

    #[ORM\Column(
        name: 'auth_key',
        type: 'string',
        length: 255
    )]
    private ?string $authKey;

    #[ORM\Column(
        name: 'created_at',
        type: 'datetime'
    )]
    private ?\DateTime $createdAt;

    #[ORM\Column(
        name: 'expires_at',
        type: 'datetime'
    )]
    private ?\DateTime $expiresAt;

    #[ORM\Column(
        name: 'user_agent',
        type: 'string',
        length: 255,
        nullable: true
    )]
    private ?string $userAgent;

    #[ORM\Column(
        name: 'user_address',
        type: 'string',
        length: 255,
        nullable: true
    )]
    private ?string $userAddress;


    public function getId(): ?int {
        return $this->id;
    }

    public function getOwner(): ?User {
        return $this->owner;
    }

    public function setOwner(?User $owner): self {
        $this->owner = $owner;
        return $this;
    }

    public function getAuthKey(): ?string {
        return $this->authKey;
    }

    public function setAuthKey(string $authKey): self {
        $this->authKey = $authKey;
        return $this;
    }

    public function getCreatedAt(): ?\DateTime {
        return $this->createdAt;
    }

    public function setCreatedAt(\DateTime $createdAt): self {
        $this->createdAt = $createdAt;
        return $this;
    }

    public function getExpiresAt(): ?\DateTime {
        return $this->expiresAt;
    }

    public function setExpiresAt(\DateTime $expiresAt): self {
        $this->expiresAt = $expiresAt;
        return $this;
    }

    public function getUserAgent(): ?string {
        return $this->userAgent;
    }

    public function setUserAgent(?string $userAgent): self {
        $this->userAgent = $userAgent;
        return $this;
    }

    public function getUserAddress(): ?string {
        return $this->userAddress;
    }

    public function setUserAddress(?string $userAddress): self {
        $this->userAddress = $userAddress;
        return $this;
    }
}
