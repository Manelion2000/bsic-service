
package com.bakouan.app.service;
import org.junit.jupiter.api.Test;import org.junit.jupiter.api.io.TempDir;import org.springframework.test.util.ReflectionTestUtils;import org.springframework.web.server.ResponseStatusException;import java.nio.file.*;import static org.assertj.core.api.Assertions.*;
class BaFileStorageServiceTest {
 @TempDir Path directory;
 private BaFileStorageService service(){var s=new BaFileStorageService();ReflectionTestUtils.setField(s,"basePath",directory.resolve("documents").toString());return s;}
 @Test void createsItsDirectoryAndReadsSavedContent(){var s=service();String id=s.save(new byte[]{1,2,3});assertThat(id).isNotBlank();assertThat(s.get(id)).containsExactly((byte)1,(byte)2,(byte)3);}
 @Test void refusesTraversalForReadWriteAndDelete()throws Exception{Path outside=directory.resolve("outside");Files.writeString(outside,"Confidentiel");var s=service();for(String id:new String[]{"../outside","..\\outside","C:\\outside","/outside",".."}){assertThatThrownBy(()->s.get(id)).isInstanceOf(ResponseStatusException.class);assertThatThrownBy(()->s.update(id,new byte[]{1})).isInstanceOf(ResponseStatusException.class);assertThatThrownBy(()->s.remove(id)).isInstanceOf(ResponseStatusException.class);}assertThat(Files.readString(outside)).isEqualTo("Confidentiel");}
 @Test void missingDocumentIsA404(){assertThatThrownBy(()->service().get("unknown")).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(404));}
}
