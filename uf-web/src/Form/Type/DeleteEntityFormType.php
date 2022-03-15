<?php
namespace App\Form\Type;

use Symfony\Component\Form\{AbstractType, FormBuilderInterface};
use Symfony\Component\Form\Extension\Core\Type\SubmitType;


class DeleteEntityFormType extends AbstractType {
    public function buildForm(FormBuilderInterface $builder, array $options) {
        $builder->add('delete', SubmitType::class);
    }
}
