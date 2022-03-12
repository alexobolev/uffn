<?php
namespace App\Twig;

use App\Entity\{Archive, Story};
use Twig\Extension\AbstractExtension;
use Twig\TwigFilter;


class StoryExtension extends AbstractExtension
{
    public function getFilters(): array {
        return [
            new TwigFilter('archive_name', [$this, 'makeArchiveName']),
            new TwigFilter('story_link', [$this, 'makeStoryLink']),
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
}
