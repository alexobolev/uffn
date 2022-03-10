<?php
namespace App\Controller;


use App\Repository\ChapterRepository;
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

    private function getVersionStoryAndChapters(int $versionId) {
        $version = $this->doctrine->getRepository(Version::class)->find(id: $versionId);
        if (!$version) {
            throw $this->createNotFoundException (
                'No version found for id = ' . $versionId
            );
        }

        $story = $version->getStory();
        if (!$story) {
            throw $this->createNotFoundException (
                'No story found for version id = ' . $versionId
            );
        }

        $chapterRepo = $this->doctrine->getRepository(Chapter::class);
        $chapters = $chapterRepo->findAllSorted($version);
        if (count($chapters) == 0) {
            throw $this->createNotFoundException (
                'No chapters found for version with id = ' . $versionId
            );
        }

        return [
          'version' => $version,
          'story' => $story,
          'chapters' => $chapters
        ];
    }
    private function makeCommonPageData(Story $story, Version $version, $chapters) {
        return [
            'story' => [
                'info' => [
                    'title' => $version->getTitle(),
                    'authors' => array_map(
                        fn($author): string => $author->getName(),
                        $version->getAuthors()->getValues()),
                    'story_id' => $story->getId(),
                    'version_id' => $version->getId(),
                    'identifier' => $story->getOriginIdentifier()
                ],
                'meta' => [
                    'rating' => $story->getRating() ?? $version->getRating(),
                    'chapter_count' => count($chapters),
                    'is_complete' => $version->getIsCompleted(),
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
            ]
        ];
    }


    /**
     * Display a single chapter with story/version info.
     * This is the default starting story display.
     *
     * @param int $id           Version instance index.
     * @param int $chapterNum   Chapter index, must belong to the version.
     */
    #[Route(
        '/versions/{id}/chapters/{chapterNum}',
        name: 'version_chapter',
        requirements: ['id' => '\d+', 'chapterNum' => '\d+']
    )]
    public function storyVersionChapter(int $id, int $chapterNum): Response {
        ['version' => $version, 'story' => $story, 'chapters' => $chapters]
            = $this->getVersionStoryAndChapters(versionId: $id);


        if ($chapterNum > count($chapters) || $chapterNum < 1) {
            throw $this->createNotFoundException (
                'Requested chapter offset ' . $chapterNum . ' is invalid'
            );
        }

        return $this->render('web/story/chapters.html.twig', array_merge_recursive (
            $this->makeCommonPageData($story, $version, $chapters), [
                'user_actions' => [
                    'nav' => [
                        'prev_index' => ($chapterNum > 1) ? $chapterNum - 1 : null,
                        'next_index' => ($chapterNum < count($chapters)) ? $chapterNum + 1 : null
                    ]
                ],
                'chapters' => [ $chapters[$chapterNum - 1] ],
                'bottom_nav' => [
                    'prev_index' => ($chapterNum > 1) ? $chapterNum - 1 : null,
                    'next_index' => ($chapterNum < count($chapters)) ? $chapterNum + 1 : null
                ]
            ]
        ));
    }


    /**
     * Display all chapters, with story/version info on top.
     *
     * @param int $id Version instance index.
     */
    #[Route(
        '/versions/{id}/fulltext',
        name: 'version_fulltext',
        requirements: ['id' => '\d+']
    )]
    public function storyVersionFullText(int $id): Response {
        ['version' => $version, 'story' => $story, 'chapters' => $chapters]
            = $this->getVersionStoryAndChapters(versionId: $id);

        return $this->render('web/story/fulltext.html.twig', array_merge_recursive (
            $this->makeCommonPageData($story, $version, $chapters), [
                'chapters' => $chapters
            ]
        ));
    }


    #[Route(
        '/versions/{id}/toc',
        name: 'version_toc',
        requirements: ['id' => '\d+']
    )]
    public function storyVersionTOC(int $id): Response {
        ['version' => $version, 'story' => $story, 'chapters' => $chapters]
            = $this->getVersionStoryAndChapters(versionId: $id);

        return $this->render('web/story/toc.html.twig', array_merge_recursive (
            $this->makeCommonPageData($story, $version, $chapters), [
                'chapters' => $chapters
            ]
        ));
    }
}
