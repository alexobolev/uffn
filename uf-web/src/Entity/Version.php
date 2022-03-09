<?php
namespace App\Entity;

use App\Repository\VersionRepository;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Doctrine\ORM\Mapping as ORM;


#[ORM\Entity(repositoryClass: VersionRepository::class)]
#[ORM\Table(name: 'story_versions')]
class Version
{
    #[ORM\Id]
    #[ORM\GeneratedValue(strategy: 'IDENTITY')]
    #[ORM\Column(
        name: 'id',
        type: 'integer'
    )]
    private $id;

    #[ORM\ManyToOne(
        targetEntity: Story::class,
        inversedBy: 'versions'
    )]
    #[ORM\JoinColumn(
        name: 'story_id',
        referencedColumnName: 'id',
        nullable: false
    )]
    private ?Story $story;

    #[ORM\Column(
        name: 'archived_at',
        type: 'datetime'
    )]
    private ?\DateTime $archivedAt;

    #[ORM\Column(
        name: 'is_hidden',
        type: 'boolean'
    )]
    private ?bool $isHidden;

    #[ORM\Column(
        name: 'title',
        type: 'string',
        length: 255
    )]
    private ?string $title;

    #[ORM\Column(
        name: 'rating',
        type: 'string',
        length: 255,
        nullable: true,
        enumType: Rating::class
    )]
    private ?string $rating;

    #[ORM\Column(
        name: 'summary',
        type: 'text',
        nullable: true
    )]
    private ?string $summary;

    #[ORM\Column(
        name: 'notes_pre',
        type: 'text',
        nullable: true
    )]
    private ?string $notesPre;

    #[ORM\Column(
        name: 'notes_post',
        type: 'text',
        nullable: true
    )]
    private ?string $notesPost;

    #[ORM\Column(
        name: 'published_at',
        type: 'datetime',
        nullable: true
    )]
    private ?\DateTime $publishedAt;

    #[ORM\Column(
        name: 'updated_at',
        type: 'datetime',
        nullable: true
    )]
    private ?\DateTime $updatedAt;

    #[ORM\Column(
        name: 'is_completed',
        type: 'boolean',
        nullable: true
    )]
    private ?bool $isCompleted;

    #[ORM\OneToMany(
        mappedBy: 'version',
        targetEntity: Author::class,
        orphanRemoval: true
    )]
    private $authors;


    #[ORM\OneToMany(
        mappedBy: 'version',
        targetEntity: Chapter::class,
        orphanRemoval: true
    )]
    private $chapters;


    public function __construct() {
        $this->authors = new ArrayCollection();
        $this->chapters = new ArrayCollection();
    }


    public function getId(): ?int {
        return $this->id;
    }

    public function getStory(): ?Story {
        return $this->story;
    }

    public function setStory(?Story $story): self {
        $this->story = $story;
        return $this;
    }

    public function getArchivedAt(): ?\DateTime {
        return $this->archivedAt;
    }

    public function setArchivedAt(\DateTime $archivedAt): self {
        $this->archivedAt = $archivedAt;
        return $this;
    }

    public function getIsHidden(): ?bool {
        return $this->isHidden;
    }

    public function setIsHidden(bool $isHidden): self {
        $this->isHidden = $isHidden;
        return $this;
    }

    public function getTitle(): ?string {
        return $this->title;
    }

    public function setTitle(string $title): self {
        $this->title = $title;
        return $this;
    }

    public function getRating(): ?string {
        return $this->rating;
    }

    public function setRating(?string $rating): self {
        $this->rating = $rating;
        return $this;
    }

    public function getSummary(): ?string {
        return $this->summary;
    }

    public function setSummary(?string $summary): self {
        $this->summary = $summary;
        return $this;
    }

    public function getNotesPre(): ?string {
        return $this->notesPre;
    }

    public function setNotesPre(?string $notesPre): self {
        $this->notesPre = $notesPre;
        return $this;
    }

    public function getNotesPost(): ?string {
        return $this->notesPost;
    }

    public function setNotesPost(string $notesPost): self {
        $this->notesPost = $notesPost;
        return $this;
    }

    public function getPublishedAt(): ?\DateTime {
        return $this->publishedAt;
    }

    public function setPublishedAt(?\DateTime $publishedAt): self {
        $this->publishedAt = $publishedAt;
        return $this;
    }

    public function getUpdatedAt(): ?\DateTime {
        return $this->updatedAt;
    }

    public function setUpdatedAt(?\DateTime $updatedAt): self {
        $this->updatedAt = $updatedAt;
        return $this;
    }

    public function getIsCompleted(): ?bool {
        return $this->isCompleted;
    }

    public function setIsCompleted(?bool $isCompleted): self {
        $this->isCompleted = $isCompleted;
        return $this;
    }

    /**
     * @return Collection<int, Author>
     */
    public function getAuthors(): Collection {
        return $this->authors;
    }

    public function addAuthor(Author $author): self {
        if (!$this->authors->contains($author)) {
            $this->authors[] = $author;
            $author->setVersion($this);
        }
        return $this;
    }

    public function removeAuthor(Author $author): self {
        if ($this->authors->removeElement($author)) {
            // set the owning side to null (unless already changed)
            if ($author->getVersion() === $this) {
                $author->setVersion(null);
            }
        }
        return $this;
    }

    /**
     * @return Collection<int, Chapter>
     */
    public function getChapters(): Collection {
        return $this->chapters;
    }

    public function addChapter(Chapter $chapter): self {
        if (!$this->chapters->contains($chapter)) {
            $this->chapters[] = $chapter;
            $chapter->setVersion($this);
        }
        return $this;
    }

    public function removeChapter(Chapter $chapter): self {
        if ($this->chapters->removeElement($chapter)) {
            // set the owning side to null (unless already changed)
            if ($chapter->getVersion() === $this) {
                $chapter->setVersion(null);
            }
        }
        return $this;
    }
}
