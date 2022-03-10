<?php
namespace App\Repository;

use App\Entity\{Chapter, Version};
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\ORM\OptimisticLockException;
use Doctrine\ORM\ORMException;
use Doctrine\Persistence\ManagerRegistry;


/**
 * @method Chapter|null find($id, $lockMode = null, $lockVersion = null)
 * @method Chapter|null findOneBy(array $criteria, array $orderBy = null)
 * @method Chapter[]    findAll()
 * @method Chapter[]    findBy(array $criteria, array $orderBy = null, $limit = null, $offset = null)
 */
class ChapterRepository extends ServiceEntityRepository {
    public function __construct(ManagerRegistry $registry) {
        parent::__construct($registry, Chapter::class);
    }

    /**
     * @throws ORMException
     * @throws OptimisticLockException
     */
    public function add(Chapter $entity, bool $flush = true): void {
        $this->_em->persist($entity);
        if ($flush) {
            $this->_em->flush();
        }
    }

    /**
     * @throws ORMException
     * @throws OptimisticLockException
     */
    public function remove(Chapter $entity, bool $flush = true): void {
        $this->_em->remove($entity);
        if ($flush) {
            $this->_em->flush();
        }
    }

    /**
     * Get all chapters, sorted in sequence order,
     * which belong to a version instance.
     */
    public function findAllSorted(Version $version) {
        return $this->createQueryBuilder('c')
            ->andWhere('c.version = :ver')
            ->setParameter('ver', $version)
            ->orderBy('c.sequenceNum', 'ASC')
            ->getQuery()
            ->getResult();
    }
}
