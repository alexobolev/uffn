<?php
namespace App\Controller;


use App\Entity\{Archive, Author, Chapter, Story, Version};
use Doctrine\Persistence\{ManagerRegistry, ObjectManager};
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;


class StoryController extends AbstractController {
    private ManagerRegistry $doctrine;

    public function __construct(ManagerRegistry $doctrine) {
        $this->doctrine = $doctrine;
    }

    #[Route(
        '/stories/{storyId}/versions',
        name: 'story_versions',
        requirements: ['storyId' => '\d+']
    )]
    public function storyVersions(int $storyId) {
        return $this->render('web/story/versions.html.twig', []);
    }


    /**
     * Display a single chapter with story/version info.
     * This is the default starting story display.
     *
     * @param int $storyId      Story instance index.
     * @param int $versionId    Version instance index, must belong to the story.
     * @param int $chapterNum   Chapter index, must belong to the version.
     */
    #[Route(
        '/stories/{storyId}/versions/{versionId}/chapters/{chapterNum}',
        name: 'story_version_chapter',
        requirements: ['storyId' => '\d+', 'versionId' => '\d+', 'chapterNum' => '\d+']
    )]
    public function storyVersionChapter(int $storyId, int $versionId, int $chapterNum) {
        $story = $this->doctrine->getRepository(Story::class)->find(id: $storyId);
        if (!$story) {
            throw $this->createNotFoundException (
                'No story found for id = ' . $storyId
            );
        }

        $version = $this->doctrine->getRepository(Version::class)->find(id: $versionId);
        if (!$version || $version->getStory() != $story) {
            throw $this->createNotFoundException (
                'No version found for id = ' . $versionId . ' or it belongs to a different story'
            );
        }

        $chapters = $version->getChapters();
        if (count($chapters) == 0) {
            throw $this->createNotFoundException (
                'No chapters found for version with id = ' . $versionId
            );
        }

        if ($chapterNum > count($chapters) || $chapterNum < 1) {
            throw $this->createNotFoundException (
                'Requested chapter offset ' . $chapterNum . ' is invalid'
            );
        }

        return $this->render('web/story/chapters.html.twig', [
            'story' => [
                'info' => [
                    'title' => $version->getTitle(),
                    'authors' => array_map(
                        fn($author): string => $author->getName(),
                        $version->getAuthors()->getValues()),
                    'story_id' => $storyId,
                    'version_id' => $versionId,
                    'identifier' => $story->getOriginIdentifier()
                ],
                'meta' => [
                    'rating' => $story->getRating() ?? $version->getRating(),
                    'chapter_count' => count($chapters),
                    'word_count' => 100500,
                    'is_complete' => $version->getIsCompleted(),
                    'updated_on' => $version->getUpdatedAt()->format('d/m/Y'),
                ],
                'custom_summary' => $story->getSummary(),
                'original_summary' => $version->getSummary()
            ],
            'user_actions' => [
                'info' => [
                    'retrieved_on' => $version->getArchivedAt()->format('d/m/Y H:i:s'),
                    'link' => sprintf('https://archiveofourown.org/works/%d', $story->getOriginIdentifier()),
                    'mobile_link_name' => sprintf('%s://%d',
                        $story->getOriginArchive()->value,
                        $story->getOriginIdentifier())
                ],
                'nav' => [
                    'prev_index' => ($chapterNum > 1) ? $chapterNum - 1 : null,
                    'next_index' => ($chapterNum < count($chapters)) ? $chapterNum + 1 : null
                ]
            ],
            'chapters' => array_slice($chapters->getValues(), $chapterNum - 1, 1),
            'bottom_nav' => [
                'prev_index' => ($chapterNum > 1) ? $chapterNum - 1 : null,
                'next_index' => ($chapterNum < count($chapters)) ? $chapterNum + 1 : null
            ]
        ]);
    }


    /**
     * Display all chapters, with story/version info on top.
     *
     * @param int $storyId      Story instance index.
     * @param int $versionId    Version instance index, must belong to the story.
     */
    #[Route(
        '/stories/{storyId}/versions/{versionId}/fulltext',
        name: 'story_version_fulltext',
        requirements: ['storyId' => '\d+', 'versionId' => '\d+']
    )]
    public function storyVersionFullText(int $storyId, int $versionId) {
        $story = $this->doctrine->getRepository(Story::class)->find(id: $storyId);
        if (!$story) {
            throw $this->createNotFoundException (
                'No story found for id = ' . $storyId
            );
        }

        $version = $this->doctrine->getRepository(Version::class)->find(id: $versionId);
        if (!$version || $version->getStory() != $story) {
            throw $this->createNotFoundException (
                'No version found for id = ' . $versionId . ' or it belongs to a different story'
            );
        }

        $chapters = $version->getChapters();
        if (count($chapters) == 0) {
            throw $this->createNotFoundException (
                'No chapters found for version with id = ' . $versionId
            );
        }

        return $this->render('web/story/fulltext.html.twig', [
            'story' => [
                'info' => [
                    'title' => $version->getTitle(),
                    'authors' => array_map(
                        fn($author): string => $author->getName(),
                        $version->getAuthors()->getValues()),
                    'story_id' => $storyId,
                    'version_id' => $versionId,
                    'identifier' => $story->getOriginIdentifier()
                ],
                'meta' => [
                    'rating' => $story->getRating() ?? $version->getRating(),
                    'chapter_count' => count($chapters),
                    'word_count' => 100500,
                    'is_complete' => $version->getIsCompleted(),
                    'updated_on' => $version->getUpdatedAt()->format('d/m/Y'),
                ],
                'custom_summary' => $story->getSummary(),
                'original_summary' => $version->getSummary()
            ],
            'user_actions' => [
                'info' => [
                    'retrieved_on' => $version->getArchivedAt()->format('d/m/Y H:i:s'),
                    'link' => sprintf('https://archiveofourown.org/works/%d', $story->getOriginIdentifier()),
                    'mobile_link_name' => sprintf('%s://%d',
                        $story->getOriginArchive()->value,
                        $story->getOriginIdentifier())
                ]
            ],
            'chapters' => $chapters
        ]);
    }


    #[Route(
        '/stories/{storyId}/versions/{versionId}/toc',
        name: 'story_version_toc',
        requirements: ['storyId' => '\d+', 'versionId' => '\d+']
    )]
    public function storyVersionTOC(int $storyId, int $versionId) {
        $story = $this->doctrine->getRepository(Story::class)->find(id: $storyId);
        if (!$story) {
            throw $this->createNotFoundException (
                'No story found for id = ' . $storyId
            );
        }

        $version = $this->doctrine->getRepository(Version::class)->find(id: $versionId);
        if (!$version || $version->getStory() != $story) {
            throw $this->createNotFoundException (
                'No version found for id = ' . $versionId . ' or it belongs to a different story'
            );
        }

        $chapters = $version->getChapters()->getValues();
        if (count($chapters) == 0) {
            throw $this->createNotFoundException (
                'No chapters found for version with id = ' . $versionId
            );
        }

        usort($chapters, fn($lhs, $rhs) => $lhs->getSequenceNum() > $rhs->getSequenceNum());

        return $this->render('web/story/toc.html.twig', [
            'story' => [
                'info' => [
                    'title' => $version->getTitle(),
                    'authors' => array_map(
                        fn($author): string => $author->getName(),
                        $version->getAuthors()->getValues()),
                    'story_id' => $storyId,
                    'version_id' => $versionId,
                    'identifier' => $story->getOriginIdentifier()
                ]
            ],
            'user_actions' => [
                'info' => [
                    'retrieved_on' => $version->getArchivedAt()->format('d/m/Y H:i:s'),
                    'link' => sprintf('https://archiveofourown.org/works/%d', $story->getOriginIdentifier()),
                    'mobile_link_name' => sprintf('%s://%d',
                        $story->getOriginArchive()->value,
                        $story->getOriginIdentifier())
                ]
            ],
            'chapters' => $chapters
        ]);
    }
}
