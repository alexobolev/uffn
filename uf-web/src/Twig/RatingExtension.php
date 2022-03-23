<?php
namespace App\Twig;

use App\Entity\Rating;
use Twig\Extension\AbstractExtension;
use Twig\TwigFilter;


class RatingExtension extends AbstractExtension
{
    public function getFilters(): array {
        return [
            new TwigFilter('rating_name', [$this, 'makeRatingName']),
            new TwigFilter('rating_class', [$this, 'makeRatingClass']),
        ];
    }

    public function makeRatingName(?Rating $rating): string {
        return $rating?->value ?? '?';
    }

    public function makeRatingClass(?Rating $rating): string {
        if ($rating === null) {
            return 'bg-dark';  // bootstrap-dependent hack, and that's ok
        }
        return match ($rating) {
            Rating::K => 'kids',
            Rating::T => 'teens',
            Rating::M => 'mature',
            Rating::E => 'explicit'
        };
    }
}
