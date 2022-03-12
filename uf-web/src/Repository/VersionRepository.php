<?php
namespace App\Repository;

use App\Entity\{Story, User, Version};
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\ORM\OptimisticLockException;
use Doctrine\ORM\ORMException;
use Doctrine\Persistence\ManagerRegistry;
use Symfony\Component\Security\Core\User\UserInterface;


/**
 * @method Version|null find($id, $lockMode = null, $lockVersion = null)
 * @method Version|null findOneBy(array $criteria, array $orderBy = null)
 * @method Version[]    findAll()
 * @method Version[]    findBy(array $criteria, array $orderBy = null, $limit = null, $offset = null)
 */
class VersionRepository extends ServiceEntityRepository {
    public function __construct(ManagerRegistry $registry) {
        parent::__construct($registry, Version::class);
    }

    /**
     * @throws ORMException
     * @throws OptimisticLockException
     */
    public function add(Version $entity, bool $flush = true): void {
        $this->_em->persist($entity);
        if ($flush) {
            $this->_em->flush();
        }
    }

    /**
     * @throws ORMException
     * @throws OptimisticLockException
     */
    public function remove(Version $entity, bool $flush = true): void {
        $this->_em->remove($entity);
        if ($flush) {
            $this->_em->flush();
        }
    }

    /**
     * Get all versions of a story, sorted in reverse archival order.
     */
    public function findAllSorted(Story $story) {
        $query = $this->_em
            ->createQuery(
                'SELECT v
                FROM App\Entity\Version v
                WHERE v.story = :story
                ORDER BY v.archivedAt DESC')
            ->setParameter('story', $story);

        return $query->getResult();
    }

    /**
     * Get most recently archived version of a story.
     */
    public function findMostRecent(Story $story) {
        $query = $this->_em
            ->createQuery(
                'SELECT v
                FROM App\Entity\Version v
                WHERE v.story = :story
                ORDER BY v.archivedAt DESC')
            ->setParameter('story', $story)
            ->setMaxResults(1);

        return $query->getSingleResult();
    }

    /**
     * Get all stories visible to the user, in reverse archival order.
     */
    public function findVisibleInArchivalOrder(UserInterface $user) {
        $query = $this->_em
            ->createQuery (
                'SELECT v
                FROM App\Entity\Version v
                JOIN v.story s
                WHERE s.isPublic = true OR s.owner = :user
                ORDER BY v.archivedAt DESC')
            ->setParameter('user', $user);
        return $query->getResult();
    }
}
