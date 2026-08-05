package romelt_techcare.backend.service.implement;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import romelt_techcare.backend.dto.NotificationDeliveryAttemptCreateRequest;
import romelt_techcare.backend.dto.NotificationDeliveryAttemptResponse;
import romelt_techcare.backend.dto.NotificationDeliveryAttemptSummaryResponse;
import romelt_techcare.backend.entity.NotificationDeliveryAttempt;
import romelt_techcare.backend.exception.PublicRequestRejectedException;
import romelt_techcare.backend.mapper.NotificationDeliveryAttemptMapper;
import romelt_techcare.backend.repository.NotificationDeliveryAttemptRepository;
import romelt_techcare.backend.service.NotificationDeliveryAttemptService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationDeliveryAttemptServiceImplementation
        implements NotificationDeliveryAttemptService {

    private final NotificationDeliveryAttemptRepository repository;

    private final NotificationDeliveryAttemptMapper mapper;

    @Override
    public NotificationDeliveryAttemptResponse create(
            NotificationDeliveryAttemptCreateRequest request
    ) {

        NotificationDeliveryAttempt entity =
                mapper.toEntity(request);

        entity = repository.save(entity);

        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationDeliveryAttemptResponse get(
            UUID notificationDeliveryAttemptId
    ) {

        return mapper.toResponse(
                find(notificationDeliveryAttemptId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationDeliveryAttemptResponse
    getLatestAttempt(
            UUID notificationDeliveryId
    ) {

        return repository
                .findFirstByNotificationDeliveryIdOrderByAttemptNumberDesc(
                        notificationDeliveryId
                )
                .map(mapper::toResponse)
                .orElseThrow(
                        () -> new PublicRequestRejectedException(
                                HttpStatus.NOT_FOUND,
                                "Notification delivery attempt not found."
                        )
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDeliveryAttemptResponse>
    getAttempts(
            UUID notificationDeliveryId
    ) {

        return repository
                .findByNotificationDeliveryIdOrderByAttemptNumberDesc(
                        notificationDeliveryId
                )
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDeliveryAttemptSummaryResponse>
    search(

            UUID notificationDeliveryId,

            String providerName,

            Boolean success,

            Boolean retryable,

            Instant startedAfter,

            Instant startedBefore,

            Pageable pageable
    ) {

        return repository.search(

                        notificationDeliveryId,

                        providerName,

                        success,

                        retryable,

                        startedAfter,

                        startedBefore,

                        pageable
                )
                .map(mapper::toSummaryResponse);
    }

    private NotificationDeliveryAttempt find(
            UUID id
    ) {

        return repository.findById(id)
                .orElseThrow(
                        () ->
                                new PublicRequestRejectedException(
                                        HttpStatus.NOT_FOUND,
                                        "Notification delivery attempt not found."
                                )
                );
    }
}