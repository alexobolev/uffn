<?php
namespace App\Twig;

use App\Entity\{Archive, Author, Story};
use Doctrine\ORM\PersistentCollection;
use Twig\Extension\AbstractExtension;
use Twig\TwigFilter;


class StoryExtension extends AbstractExtension
{
    public function getFilters(): array {
        return [
            new TwigFilter('archive_name', [$this, 'makeArchiveName']),
            new TwigFilter('story_link', [$this, 'makeStoryLink']),
            new TwigFilter('author_list', [$this, 'makeAuthorList']),
        ];
    }

    public function makeArchiveName(?Archive $archive): string {
        return $archive?->value;
    }

    public function makeStoryLink(?Story $story): string {
        $urlTemplate = match ($story->getOriginArchive()) {
            Archive::AO3 => 'https://archiveofourown.org/works/%s',
            Archive::FFN => 'https://www.fanfiction.net/s/%s',
        };
        return sprintf($urlTemplate, $story->getOriginIdentifier());
    }

    public function makeAuthorList(PersistentCollection $authors): string {
        $names = array_map(fn(Author $author): string => $author->getName(), $authors->getValues());
        return implode(', ', $names);
    }
}
