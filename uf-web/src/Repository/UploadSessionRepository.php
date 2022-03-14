<?php
namespace App\Repository;

use App\Entity\UploadSession;
use App\Entity\User;
use Doctrine\Bundle\DoctrineBundle\Repository\ServiceEntityRepository;
use Doctrine\ORM\NonUniqueResultException;
use Doctrine\ORM\OptimisticLockException;
use Doctrine\ORM\ORMException;
use Doctrine\ORM\Query;
use Doctrine\Persistence\ManagerRegistry;


/**
 * @method UploadSession|null find($id, $lockMode = null, $lockVersion = null)
 * @method UploadSession|null findOneBy(array $criteria, array $orderBy = null)
 * @method UploadSession[]    findAll()
 * @method UploadSession[]    findBy(array $criteria, array $orderBy = null, $limit = null, $offset = null)
 */
class UploadSessionRepository extends ServiceEntityRepository {
    public function __construct(ManagerRegistry $registry) {
        parent::__construct($registry, UploadSession::class);
    }

    /**
     * @throws ORMException
     * @throws OptimisticLockException
     */
    public function add(UploadSession $entity, bool $flush = true): void {
        $this->_em->persist($entity);
        if ($flush) {
            $this->_em->flush();
        }
    }

    /**
     * @throws ORMException
     * @throws OptimisticLockException
     */
    public function remove(UploadSession $entity, bool $flush = true): void {
        $this->_em->remove($entity);
        if ($flush) {
            $this->_em->flush();
        }
    }

    /**
     * Find a session which can be used by the given user
     * with the given session key, i.e. which is not expired.
     *
     * @param User $owner
     * @param string $key
     *
     * @return float|int|mixed|string|null
     */
    public function findUsableOwnedBy(User $owner, string $key) {
        $queryText = '
            SELECT s
            FROM App\Entity\UploadSession s
            WHERE s.owner = :owner
                AND s.authKey = :key
                AND s.expiresAt >= CURRENT_TIMESTAMP()
        ';

        $query = $this->_em
            ->createQuery($queryText)
            ->setParameter('owner', $owner)
            ->setParameter('key', $key);

        try {
            return $query->getOneOrNullResult();
        } catch (NonUniqueResultException $exception) {
            return null;
        }
    }

    public function removeByKeyOwnedBy(User $owner, string $key) {
        $query = $this->_em
            ->createQuery('
                DELETE App\Entity\UploadSession s
                WHERE s.owner = :owner AND s.authKey = :key
            ')
            ->setParameter('owner', $owner)
            ->setParameter('key', $key);

        $query->execute();
    }

    /**
     * Remove all stale sessions.
     * @return void
     */
    public function removeStale(): void {
        $query = $this->_em
            ->createQuery('
                DELETE App\Entity\UploadSession s
                WHERE s.expiresAt < CURRENT_TIMESTAMP()
        ');

        $query->execute();
    }

    /**
     * Remove stale sessions for a user.
     * @param User $owner
     * @return void
     */
    public function removeStaleOwnedBy(User $owner): void {
        $query = $this->_em
            ->createQuery('
                DELETE App\Entity\UploadSession s
                WHERE s.owner = :owner
                    AND s.expiresAt < CURRENT_TIMESTAMP()
            ')
            ->setParameter('owner', $owner);

        $query->execute();
    }

    /**
     * Remove all sessions for a user.
     * @param User $owner
     * @return void
     */
    public function removeAllOwnedBy(User $owner): void {
        $query = $this->_em
            ->createQuery('
                DELETE App\Entity\UploadSession s
                WHERE s.owner = :owner
            ')
            ->setParameter('owner', $owner);

        $query->execute();
    }
}
