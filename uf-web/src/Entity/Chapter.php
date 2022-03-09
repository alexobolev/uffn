<?php
namespace App\Entity;

use App\Repository\ChapterRepository;
use Doctrine\ORM\Mapping as ORM;


#[ORM\Entity(repositoryClass: ChapterRepository::class)]
class Chapter
{
    #[ORM\Id]
    #[ORM\GeneratedValue(strategy: 'IDENTITY')]
    #[ORM\Column(
        name: 'id',
        type: 'integer'
    )]
    private ?int $id;

    #[ORM\ManyToOne(
        targetEntity: Version::class,
        inversedBy: 'chapters'
    )]
    #[ORM\JoinColumn(
        name: 'story_version_id',
        referencedColumnName: 'id',
        nullable: false
    )]
    private ?Version $version;

    #[ORM\Column(
        name: 'sequence_num',
        type: 'integer'
    )]
    private ?int $sequenceNum;

    #[ORM\Column(
        name: 'title',
        type: 'string',
        length: 255,
        nullable: true
    )]
    private ?string $title;

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
        name: 'contents',
        type: 'text'
    )]
    private ?string $contents;


    public function getId(): ?int {
        return $this->id;
    }

    public function getVersion(): ?Version {
        return $this->version;
    }

    public function setVersion(?Version $version): self {
        $this->version = $version;
        return $this;
    }

    public function getSequenceNum(): ?int {
        return $this->sequenceNum;
    }

    public function setSequenceNum(int $sequenceNum): self {
        $this->sequenceNum = $sequenceNum;
        return $this;
    }

    public function getTitle(): ?string {
        return $this->title;
    }

    public function setTitle(?string $title): self {
        $this->title = $title;
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

    public function setNotesPost(?string $notesPost): self {
        $this->notesPost = $notesPost;
        return $this;
    }

    public function getContents(): ?string {
        return $this->contents;
    }

    public function setContents(string $contents): self {
        $this->contents = $contents;
        return $this;
    }
}
