<?php
namespace App\Entity;

use App\Repository\StoryRepository;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Doctrine\ORM\Mapping as ORM;


#[ORM\Entity(repositoryClass: StoryRepository::class)]
#[ORM\Table(name: 'stories')]
class Story
{
    #[ORM\Id]
    #[ORM\GeneratedValue(strategy: 'IDENTITY')]
    #[ORM\Column(
        name: 'id',
        type: 'integer'
    )]
    private ?int $id;

    #[ORM\Column(
        name: 'origin_identifier',
        type: 'string',
        length: 255
    )]
    private ?string $originIdentifier;

    #[ORM\Column(
        name: 'origin_archive',
        type: 'string',
        length: 255,
        enumType: Archive::class
    )]
    private ?string $originArchive;

    #[ORM\Column(
        name: 'is_public',
        type: 'boolean'
    )]
    private ?bool $isPublic;

    #[ORM\ManyToOne(targetEntity: User::class)]
    #[ORM\JoinColumn(
        name: "owner_id",
        referencedColumnName: "id",
        nullable: false
    )]
    private ?User $owner;

    #[ORM\Column(
        name: 'owner_summary',
        type: 'text',
        nullable: true
    )]
    private ?string $summary;

    #[ORM\Column(
        name: 'owner_rating',
        type: 'string',
        length: 255,
        nullable: true,
        enumType: Rating::class
    )]
    private ?string $rating;

    #[ORM\OneToMany(
        mappedBy: 'story',
        targetEntity: Version::class,
        orphanRemoval: true
    )]
    private $versions;


    public function __construct() {
        $this->versions = new ArrayCollection();
    }

    public function getId(): ?int {
        return $this->id;
    }

    public function getOriginIdentifier(): ?string {
        return $this->originIdentifier;
    }

    public function setOriginIdentifier(string $originIdentifier): self {
        $this->originIdentifier = $originIdentifier;
        return $this;
    }

    public function getOriginArchive(): ?string {
        return $this->originArchive;
    }

    public function setOriginArchive(string $originArchive): self {
        $this->originArchive = $originArchive;
        return $this;
    }

    public function getIsPublic(): ?bool {
        return $this->isPublic;
    }

    public function setIsPublic(bool $isPublic): self {
        $this->isPublic = $isPublic;
        return $this;
    }

    public function getOwner(): ?User {
        return $this->owner;
    }

    public function setOwner(?User $owner): self {
        $this->owner = $owner;
        return $this;
    }

    public function getSummary(): ?string {
        return $this->summary;
    }

    public function setSummary(?string $summary): self {
        $this->summary = $summary;
        return $this;
    }

    public function getRating(): ?string {
        return $this->rating;
    }

    public function setRating(?string $rating): self {
        $this->rating = $rating;
        return $this;
    }

    /**
     * @return Collection<int, Version>
     */
    public function getVersions(): Collection {
        return $this->versions;
    }

    public function addVersion(Version $version): self {
        if (!$this->versions->contains($version)) {
            $this->versions[] = $version;
            $version->setStory($this);
        }
        return $this;
    }

    public function removeVersion(Version $version): self {
        if ($this->versions->removeElement($version)) {
            // set the owning side to null (unless already changed)
            if ($version->getStory() === $this) {
                $version->setStory(null);
            }
        }

        return $this;
    }
}
