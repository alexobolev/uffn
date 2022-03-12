<?php
namespace App\Form\Type;

use App\Form\Model\PasswordChangeFormModel;
use Symfony\Component\Form\{AbstractType, FormBuilderInterface};
use Symfony\Component\Form\Extension\Core\Type\{EmailType, PasswordType, RepeatedType, SubmitType, TextType};
use Symfony\Component\OptionsResolver\{Options, OptionsResolver};
use Symfony\Component\Security\Core\Validator\Constraints as SecurityAssert;
use Symfony\Component\Validator\Constraints as Assert;


class PasswordChangeFormType extends AbstractType {

    public function buildForm(FormBuilderInterface $builder, array $options) {
        $builder
            ->add('email', EmailType::class, [
                'required' => false,
                'disabled' => true,
                'constraints' => [
                    new Assert\NotBlank(['message' => 'User email must not be blank']),
                    // new Assert\Email(['message' => 'User email must be textually valid'])
                    // email is not checked to allow local addresses, e.g. admin@localhost
                ]
            ])
            ->add('login', TextType::class, [
                'required' => false,
                'disabled' => true,
                'constraints' => [
                    new Assert\NotBlank(['message' => 'User login must not be blank'])
                ]
            ])
            ->add('old_password', PasswordType::class, [
                'required' => true,
                'constraints' => [
                    new Assert\NotBlank(['message' => 'Current password must not be blank']),
                    new SecurityAssert\UserPassword(['message' => 'Current password is incorrect'])
                ]
            ])
            ->add('new_password', RepeatedType::class, [
                'type' => PasswordType::class,
                'invalid_message' => 'new password fields must match',
                'required' => true,
                'constraints' => [
                    new Assert\NotBlank([
                        'message' => 'New password must not be blank'
                    ]),
                    new Assert\Length([
                        'min' => 6,
                        'max' => 60,
                        'minMessage' => 'New password must be at least {{ limit }} characters long',
                        'maxMessage' => 'New password must be at most {{ limit }} characters long'
                    ])
                ]
            ])
            ->add('update', SubmitType::class)
        ;
    }

    public function configureOptions(OptionsResolver $resolver): void {
        $resolver->setDefaults([
            'data_class' => PasswordChangeFormModel::class,
        ]);
    }
}
