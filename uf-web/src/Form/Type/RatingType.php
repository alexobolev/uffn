<?php
namespace App\Form\Type;

use App\Entity\Rating;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\OptionsResolver\Options;
use Symfony\Component\OptionsResolver\OptionsResolver;


class RatingType extends AbstractType {
    public function configureOptions(OptionsResolver $resolver): void {
        $resolver
            ->setDefault('choices', static function (Options $options): array {
                return array_merge([ null ], Rating::cases());
            })
            ->setDefault('choice_label', static function (?\UnitEnum $choice): string {
                $names = [
                    null => 'None / default',
                    'K' => 'Kids / General Audiences',
                    'T' => 'Teenagers',
                    'M' => 'Mature audiences',
                    'E' => 'Explicit'
                ];
                return $names[$choice?->name];
            })
            ->setDefault('choice_value', static function (Options $options): ?\Closure {
                return static function (?\BackedEnum $choice): ?string {
                    return $choice?->name;
                };
            })
        ;
    }

    public function getParent(): string {
        return ChoiceType::class;
    }
}
