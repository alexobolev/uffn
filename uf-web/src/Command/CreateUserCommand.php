<?php
namespace App\Command;

use App\Entity\User;
use Doctrine\Persistence\ManagerRegistry;
use Symfony\Component\Console\Attribute\AsCommand;
use Symfony\Component\Console\Command\Command;
use Symfony\Component\Console\Input\InputArgument;
use Symfony\Component\Console\Input\InputInterface;
use Symfony\Component\Console\Output\OutputInterface;
use Symfony\Component\Console\Question\Question;
use Symfony\Component\PasswordHasher\Hasher\UserPasswordHasherInterface;


#[AsCommand(
    name: 'uf:create-user',
    description: 'Creates a user',
    hidden: false
)]
class CreateUserCommand extends Command {
    private ManagerRegistry $doctrine;
    private UserPasswordHasherInterface $passwordHasher;

    public function __construct(ManagerRegistry $doctrine, UserPasswordHasherInterface $passwordHasher) {
        $this->doctrine = $doctrine;
        $this->passwordHasher = $passwordHasher;
        parent::__construct();
    }

    protected function configure(): void {
        $this
            ->addArgument('login', InputArgument::REQUIRED, 'Short unique name for the user.')
            ->addArgument('email', InputArgument::REQUIRED, 'Unique (and working) email to use for authentication')
        ;
    }

    protected function execute(InputInterface $input, OutputInterface $output): int {
        $login = $input->getArgument('login');
        $email = $input->getArgument('email');

        $output->writeln("Creating a user with the following params:");
        $output->writeln("  login:  " . $input->getArgument('login'));
        $output->writeln("  email:  " . $input->getArgument('email'));

        $existingUsers = $this->doctrine
            ->getRepository(User::class)
            ->findWithAnyOfCredentials($login, $email);

        if (count($existingUsers) > 0) {
            $output->writeln("Can't create a user with this parameters:");
            $output->writeln("  either of the credentials is already in use!");
            return Command::INVALID;
        }

        $inputHelper = $this->getHelper('question');
        $pwdQuestion = new Question('Enter user password: ');
        $pwdQuestion->setHidden(true);
        $pwdQuestion->setHiddenFallback(false);
        $pwdQuestion->setValidator(function ($pwd) {
            $pwd = trim((string)$pwd);
            if (strlen($pwd) < 6 || strlen($pwd) > 60) {
                throw new \RuntimeException('Password should be a non-empty string of 6-60 characters');
            }
            return $pwd;
        });
        $pwdQuestion->setMaxAttempts(3);

        $password = $inputHelper->ask($input, $output, $pwdQuestion);
        $user = (new User())
            ->setLogin($login)
            ->setEmail($email)
            ->setIsAdmin(true)
            ->setRegisteredAt(new \DateTime());

        $password = $this->passwordHasher->hashPassword($user, $password);
        $user->setPassword($password);

        $entityManager = $this->doctrine->getManager();
        $entityManager->persist($user);
        $entityManager->flush();

        $output->writeln('Created a new user with id = ' . $user->getId());
        return Command::SUCCESS;
    }
}
