package org.gso.brinder.profile.endpoint;

import java.util.List;

import org.gso.brinder.common.dto.PageDto;
import org.gso.brinder.profile.dto.ProfileDto;
import org.gso.brinder.profile.model.ProfileModel;
import org.gso.brinder.profile.service.ProfileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.github.rutledgepaulv.qbuilders.builders.GeneralQueryBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.visitors.MongoVisitor;
import com.github.rutledgepaulv.rqe.pipes.QueryConversionPipeline;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(value = ProfileController.PATH, produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ProfileController {

    public static final String PATH = "/api/v1/profiles";
    public static int MAX_PAGE_SIZE = 200;

    private final ProfileService profileService;
    private QueryConversionPipeline pipeline = QueryConversionPipeline.defaultPipeline();

    @PostMapping(consumes = { MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<ProfileDto> createProfile(@RequestBody ProfileDto profileDto) {
        ProfileDto createdProdile = profileService.createProfile(profileDto.toModel()).toDto();
        return ResponseEntity
                .created(
                        ServletUriComponentsBuilder.fromCurrentContextPath()
                                .path(createdProdile.getId())
                                .build()
                                .toUri())
                .body(createdProdile);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfileDto> getProfile(@PathVariable("id") @NonNull String profileId) {
        return ResponseEntity.ok(profileService.getProfile(profileId).toDto());
    }

    @PutMapping(path = "/{id}", consumes = { MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<ProfileDto> updateProfile(@PathVariable @NonNull String profileId,
            @RequestBody @NonNull ProfileDto profileDto) {
        profileDto.setId(profileId);
        return ResponseEntity.ok(profileService.updateProfile(profileDto.toModel()).toDto());
    }

    @GetMapping
    public ResponseEntity<PageDto<ProfileDto>> searchProfile(@RequestParam(required = false) String query,
            @PageableDefault(size = 20) Pageable pageable) {
        Pageable checkedPageable = checkPageSize(pageable);
        Criteria criteria = convertQuery(query);
        Page<ProfileModel> results = profileService.searchProfiles(criteria, checkedPageable);
        PageDto<ProfileDto> pageResults = toPageDto(results);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(pageResults);
    }

    @GetMapping(params = "mail")
    public ResponseEntity<PageDto<ProfileDto>> searchByMail(@RequestParam String mail,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ProfileModel> results = profileService.searchByMail(mail, pageable);
        PageDto<ProfileDto> pageResults = toPageDto(results);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(pageResults);
    }

    @GetMapping("/current")
    public ResponseEntity getCurrentUserProfile(@AuthenticationPrincipal Jwt principal) {
        Object email = principal.getClaims().get("email");
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        Page<ProfileModel> profilePage = profileService.searchByMail(email.toString(), PageRequest.of(0, 1));
        
        // Check if there's at least one profile found
        if(profilePage.hasContent()) {
            return ResponseEntity.ok(profilePage.getContent().get(0).toDto());
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    /**
     * Convertit une "requête RSQL en un objet Criteria compréhensible par la base
     *
     * @param stringQuery
     * @return
     */
    private Criteria convertQuery(String stringQuery) {
        Criteria criteria;
        if (StringUtils.hasText(stringQuery)) {
            Condition<GeneralQueryBuilder> condition = pipeline.apply(stringQuery, ProfileModel.class);
            criteria = condition.query(new MongoVisitor());
        } else {
            criteria = new Criteria();
        }
        return criteria;
    }

    private Pageable checkPageSize(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            return PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE);
        }
        return pageable;
    }

    private PageDto<ProfileDto> toPageDto(Page<ProfileModel> results) {
        List<ProfileDto> profiles = results.map(ProfileModel::toDto).toList();
        PageDto<ProfileDto> pageResults = new PageDto<>();
        pageResults.setData(profiles);
        pageResults.setTotalElements(results.getTotalElements());
        pageResults.setPageSize(results.getSize());
        if (results.hasNext()) {
            results.nextOrLastPageable();
            pageResults.setNext(
                    ServletUriComponentsBuilder.fromCurrentContextPath()
                            .queryParam("page", results.nextOrLastPageable().getPageNumber())
                            .queryParam("size", results.nextOrLastPageable().getPageSize())
                            .build().toUri());
        }
        pageResults.setFirst(
                ServletUriComponentsBuilder.fromCurrentContextPath()
                        .queryParam("page", results.previousOrFirstPageable().getPageNumber())
                        .queryParam("size", results.previousOrFirstPageable().getPageSize())
                        .build().toUri());
        pageResults.setLast(
                ServletUriComponentsBuilder.fromCurrentContextPath()
                        .queryParam("page", results.nextOrLastPageable().getPageNumber())
                        .queryParam("size", results.nextOrLastPageable().getPageSize())
                        .build().toUri());
        return pageResults;
    }
}
