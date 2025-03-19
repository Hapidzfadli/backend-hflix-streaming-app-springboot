package com.hapidzfadli.hflix.domain.repository;

import com.hapidzfadli.hflix.domain.entity.User;
import com.hapidzfadli.hflix.domain.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {

    /**
     * Finds all videos owned by a specific user.
     * Used to display a list of videos owned by the user.
     *
     * @param user The user whose videos are to be found.
     * @return A list of videos owned by the specific user.
     */
    List<Video> findByUser(User user);

    /**
     * Finds videos owned by a user with pagination.
     * Used to display a paginated list of videos owned by the user.
     *
     * @param user The user whose videos are to be found.
     * @param pageable Pagination information.
     * @return A paginated list of videos owned by the specific user.
     */
    Page<Video> findByUser(User user, Pageable pageable);

    /**
     * Finds videos based on their status (UPLOADING, PROCESSING, READY, ERROR).
     * Used for monitoring and filtering videos based on their status.
     *
     * @param status The status of the videos.
     * @param pageable Pagination information.
     * @return A paginated list of videos matching the specific status.
     */
    Page<Video> findByStatus(Video.Status status, Pageable pageable);

    /**
     * Finds videos based on their visibility (PUBLIC, PRIVATE, UNLISTED).
     * Used for filtering videos based on their privacy settings.
     *
     * @param visibility The visibility of the videos.
     * @param pageable Pagination information.
     * @return A paginated list of videos matching the specific visibility.
     */
    Page<Video> findByVisibility(Video.Visibility visibility, Pageable pageable);

    /**
     * Searches for public videos based on a keyword.
     * Used for the video search feature by visitors.
     * Only searches videos that are PUBLIC and READY.
     *
     * @param keyword The search keyword.
     * @param pageable Pagination information.
     * @return A paginated list of public videos matching the search keyword.
     */
    @Query("SELECT v FROM Video v WHERE " +
            "v.visibility = 'PUBLIC' AND " +
            "v.status = 'READY' AND " +
            "(:keyword IS NULL OR LOWER(v.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(v.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Video> searchPublicVideos(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Searches for videos owned by a specific user based on a keyword.
     * Used for the search feature in the user's dashboard.
     * Users can search through all their own videos.
     *
     * @param user The user whose videos are to be searched.
     * @param keyword The search keyword.
     * @param pageable Pagination information.
     * @return A paginated list of the user's videos matching the search keyword.
     */
    @Query("SELECT v FROM Video v WHERE " +
            "v.user = :user AND " +
            "(:keyword IS NULL OR LOWER(v.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(v.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Video> searchUserVideos(@Param("user") User user, @Param("keyword") String keyword, Pageable pageable);

    Page<Video> findByVisibilityAndStatus(Video.Visibility visibility, Video.Status status, Pageable pageable);
}