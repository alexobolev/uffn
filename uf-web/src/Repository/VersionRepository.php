<?php
namespace App\Repository;

use Doctrine\ORM\NativeQuery;
use Doctrine\ORM\Query;
use Doctrine\ORM\Query\ResultSetMapping;
use Doctrine\ORM\Query\ResultSetMappingBuilder;
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
    public function findMostRecent(Story $story): Version {
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


    private function makeLastOfStoryUploadedInReverseArchivalOrderQuery(
        User $owner, ?int $offset = null, ?int $limit = null): NativeQuery
    {
        $rsm = new ResultSetMappingBuilder($this->_em);
        $rsm->addRootEntityFromClassMetadata(Version::class, 'vl');

        $sql = '
            SELECT vl.* FROM stories s
            LEFT JOIN LATERAL (
                SELECT * FROM story_versions v
                WHERE v.story_id = s.id
                ORDER BY v.archived_at DESC
                LIMIT 1
            ) vl ON vl.story_id = s.id
            WHERE s.owner_id = :owner
            ORDER BY vl.archived_at DESC
        ';

        if ($offset !== null) {
            $sql .= " OFFSET :offset";
        }

        if ($limit !== null) {
            $sql .= " LIMIT :limit";
        }

        $query = $this->_em->createNativeQuery($sql, $rsm);
        $query->setParameter("owner", $owner->getId());

        if ($offset !== null) {
            $query->setParameter('offset', $offset);
        }

        if ($limit !== null) {
            $query->setParameter('limit', $limit);
        }

        return $query;
    }

    private function makeLastOfStoryUploadedCountQuery(User $owner): NativeQuery {
        $rsm = new ResultSetMappingBuilder($this->_em);
        $rsm->addScalarResult('count', 'count');

        $sql = '
            SELECT COUNT(*) FROM stories s
            LEFT JOIN LATERAL (
                SELECT * FROM story_versions v
                WHERE v.story_id = s.id
                ORDER BY v.archived_at DESC
                LIMIT 1
            ) vl ON vl.story_id = s.id
            WHERE s.owner_id = :owner
        ';

        $query = $this->_em->createNativeQuery($sql, $rsm);
        $query->setParameter("owner", $owner->getId());

        return $query;
    }

    public function findLastOfStoryUploadedInReverseArchivalOrder(User $owner) {
        $query = $this->makeLastOfStoryUploadedInReverseArchivalOrderQuery($owner);
        return $query->getResult();
    }

    public function findLastOfStoryUploadedInReverseArchivalOrderLimited(User $owner, int $offset, int $limit) {
        $query = $this->makeLastOfStoryUploadedInReverseArchivalOrderQuery($owner, $offset, $limit);
        return $query->getResult();
    }

    public function countLastOfStoryUploaded(User $owner): int {
        $query = $this->makeLastOfStoryUploadedCountQuery($owner);
        return $query->getSingleScalarResult();
    }
}
