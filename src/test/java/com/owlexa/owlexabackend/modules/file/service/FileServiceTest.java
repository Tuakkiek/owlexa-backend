package com.owlexa.owlexabackend.modules.file.service;

import com.owlexa.owlexabackend.common.context.TenantContext;
import com.owlexa.owlexabackend.common.exception.BadRequestException;
import com.owlexa.owlexabackend.modules.file.config.FileStorageProperties;
import com.owlexa.owlexabackend.modules.file.entity.FileType;
import com.owlexa.owlexabackend.modules.file.entity.StorageProvider;
import com.owlexa.owlexabackend.modules.file.entity.StoredFile;
import com.owlexa.owlexabackend.modules.file.mapper.FileMapper;
import com.owlexa.owlexabackend.modules.file.repository.FileReferenceRepository;
import com.owlexa.owlexabackend.modules.file.repository.StoredFileRepository;
import com.owlexa.owlexabackend.modules.file.storage.FileStorage;
import com.owlexa.owlexabackend.modules.file.storage.StoredObject;
import com.owlexa.owlexabackend.modules.user.entity.Center;
import com.owlexa.owlexabackend.modules.user.entity.User;
import com.owlexa.owlexabackend.modules.user.repository.CenterRepository;
import com.owlexa.owlexabackend.modules.user.service.AuthorizationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock private StoredFileRepository storedFileRepository;
    @Mock private FileReferenceRepository fileReferenceRepository;
    @Mock private CenterRepository centerRepository;
    @Mock private AuthorizationService authorizationService;
    @Mock private FileStorage fileStorage;

    private FileService service;

    @BeforeEach
    void setUp() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setMaxSize(1024 * 1024);
        service = new FileService(
                storedFileRepository,
                fileReferenceRepository,
                centerRepository,
                authorizationService,
                fileStorage,
                properties,
                new FileMapper()
        );
        TenantContext.setCurrentTenantId(10L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void upload_detectsPngFromContentAndStoresMetadata() throws Exception {
        byte[] png = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
        };
        MockMultipartFile upload =
                new MockMultipartFile("file", "diagram.png", "image/png", png);
        Center center = new Center();
        center.setId(10L);
        User user = new User();
        user.setId(20L);

        when(authorizationService.getCurrentUser()).thenReturn(user);
        when(centerRepository.findById(10L)).thenReturn(Optional.of(center));
        when(fileStorage.store(anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenReturn(new StoredObject(
                        "2026/07/random.png",
                        "http://localhost:8081/uploads/2026/07/random.png",
                        StorageProvider.LOCAL
                ));
        when(storedFileRepository.save(any(StoredFile.class))).thenAnswer(invocation -> {
            StoredFile file = invocation.getArgument(0);
            file.setId(15L);
            return file;
        });

        var response = service.upload(upload);

        assertThat(response.getId()).isEqualTo(15L);
        assertThat(response.getType()).isEqualTo(FileType.IMAGE);
        assertThat(response.getMimeType()).isEqualTo("image/png");
        ArgumentCaptor<StoredFile> captor = ArgumentCaptor.forClass(StoredFile.class);
        verify(storedFileRepository).save(captor.capture());
        assertThat(captor.getValue().getStoredName())
                .matches("[0-9a-f-]{36}\\.png");
        assertThat(captor.getValue().getOriginalName()).isEqualTo("diagram.png");
    }

    @Test
    void upload_rejectsExecutableRenamedAsImage() {
        byte[] executableHeader = new byte[] {
                0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00
        };
        MockMultipartFile upload =
                new MockMultipartFile("file", "malware.png", "image/png", executableHeader);
        Center center = new Center();
        center.setId(10L);
        when(authorizationService.getCurrentUser()).thenReturn(new User());
        when(centerRepository.findById(10L)).thenReturn(Optional.of(center));

        assertThatThrownBy(() -> service.upload(upload))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("content");
    }

    @ParameterizedTest
    @MethodSource("supportedRichTextFiles")
    void upload_acceptsEveryRichTextMediaCategory(
            String fileName,
            String declaredMimeType,
            byte[] content,
            FileType expectedType
    ) throws Exception {
        MockMultipartFile upload =
                new MockMultipartFile("file", fileName, declaredMimeType, content);
        Center center = new Center();
        center.setId(10L);
        User user = new User();
        user.setId(20L);

        when(authorizationService.getCurrentUser()).thenReturn(user);
        when(centerRepository.findById(10L)).thenReturn(Optional.of(center));
        when(fileStorage.store(anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenAnswer(invocation -> new StoredObject(
                        invocation.getArgument(0),
                        "http://localhost:8081/uploads/" + invocation.getArgument(0),
                        StorageProvider.LOCAL
                ));
        when(storedFileRepository.save(any(StoredFile.class))).thenAnswer(invocation -> {
            StoredFile file = invocation.getArgument(0);
            file.setId(21L);
            return file;
        });

        var response = service.upload(upload);

        assertThat(response.getType()).isEqualTo(expectedType);
        assertThat(response.getOriginalName()).isEqualTo(fileName);
    }

    private static Stream<Arguments> supportedRichTextFiles() {
        return Stream.of(
                Arguments.of(
                        "lesson.mp3",
                        "audio/mpeg",
                        new byte[] {0x49, 0x44, 0x33, 0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
                        FileType.AUDIO
                ),
                Arguments.of(
                        "lesson.mp4",
                        "video/mp4",
                        new byte[] {
                                0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70,
                                0x6D, 0x70, 0x34, 0x32, 0x00, 0x00, 0x00, 0x00,
                                0x6D, 0x70, 0x34, 0x32, 0x69, 0x73, 0x6F, 0x6D
                        },
                        FileType.VIDEO
                ),
                Arguments.of(
                        "handout.pdf",
                        "application/pdf",
                        "%PDF-1.7\n1 0 obj\n<<>>\nendobj\n%%EOF".getBytes(),
                        FileType.PDF
                ),
                Arguments.of(
                        "resources.zip",
                        "application/zip",
                        new byte[] {
                                0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x00, 0x00,
                                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
                        },
                        FileType.ATTACHMENT
                )
        );
    }
}
