<?php
namespace App\Entity;

use App\Repository\AuthorRepository;
use Doctrine\ORM\Mapping as ORM;


#[ORM\Entity(repositoryClass: AuthorRepository::class)]
#[ORM\Table(name: 'story_version_authors')]
class Author
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
        inversedBy: 'authors'
    )]
    #[ORM\JoinColumn(
        name: 'story_version_id',
        referencedColumnName: 'id',
        nullable: false
    )]
    private ?Version $version;

    #[ORM\Column(
        name: 'name',
        type: 'string',
        length: 255
    )]
    private ?string $name;

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

    public function getName(): ?string {
        return $this->name;
    }

    public function setName(string $name): self {
        $this->name = $name;
        return $this;
    }
}
