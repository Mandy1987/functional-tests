package nl.vpro.poms.npoapi;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import nl.vpro.domain.api.Error;
import nl.vpro.domain.api.IdList;
import nl.vpro.domain.api.MultipleMediaResult;
import nl.vpro.domain.api.media.MediaForm;
import nl.vpro.domain.api.profile.Profile;
import nl.vpro.domain.media.MediaObject;
import nl.vpro.poms.AbstractApiTest;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

@RunWith(Parameterized.class)
@Slf4j
public class ApiMediaLoadTest extends AbstractApiTest {

    private final String profileName;
    private Profile profile;
    private final List<String> mids;

    public ApiMediaLoadTest(String profileName, List<String> mids, MediaType mediaType) {
        this.profileName = profileName;
        this.mids = mids;
        clients.setAccept(mediaType);

    }

    @Before
    public void setup() {
        if (profileName != null) {
            profile = clients.getProfileService().load(profileName, null);
        }
    }


    @Parameterized.Parameters
    public static Collection<Object[]> getMediaObjects() throws IOException {
        List<Object[]> result = new ArrayList<>();
        for (MediaType mediaType : Arrays.asList(MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_XML_TYPE)) {
            for (String profile : Arrays.asList(null, "vpro")) {
                List<String> mids = new ArrayList<>();
                mids.add("VPWON_1181223"); // NPA-341 ?
                try {
                    mids.addAll(clients.getMediaService().find(new MediaForm(), profile, "", 0L, 10).asResult().stream().map(MediaObject::getMid).collect(Collectors.toList()));
                    if (mids.size() == 0) {
                        throw new IllegalStateException("No media found for profile " + profile);
                    }

                } catch (javax.ws.rs.ServiceUnavailableException ue) {
                    log.warn(ue.getMessage());
                }
                result.add(new Object[]{profile, mids, mediaType});

            }
        }
        return result;
    }

    @Test
    public void load() throws Exception {
        assumeTrue(mids.size() > 0);
        MediaObject o = clients.getMediaService().load(mids.get(0), null, null);
        assertThat(o.getMid()).isEqualTo(mids.get(0));
        if (profileName != null) {
            assertThat(profile.getMediaProfile().test(o)).isTrue();
        }
    }

    @Test
    public void loadWithPropertiesNone() throws Exception {
        assumeTrue(mids.size() > 0);
        MediaObject o = clients.getMediaService().load(mids.get(0), "none", null);
        assertThat(o.getMid()).isEqualTo(mids.get(0));
    }

    @Test
    public void loadInProfile() throws Exception {
        Assume.assumeNotNull(profileName);
        assumeTrue(mids.size() > 0);
        MediaObject o = clients.getMediaService().load(mids.get(0), null, profileName);
        assertThat(o.getMid()).isEqualTo(mids.get(0));
    }


    @Test
    public void loadOutsideProfile() throws Exception {
        Assume.assumeNotNull(profileName);
        assumeTrue(mids.size() > 0);
        try {
            clients.setAcceptableLanguages(Arrays.asList(Locale.US));
            clients.getMediaService().load(mids.get(0), null, "eo");
        } catch (NotFoundException nfe) {
            assertThat(nfe.getResponse().getEntity()).isInstanceOf(Error.class);
            Error error = (Error) nfe.getResponse().getEntity();
            assertThat(error.getMessage()).contains("is niet van de omroep EO");
            assertThat(error.getTestResult().getDescription().getValue()).contains("is niet van de omroep EO");

            return;
        }
        throw new AssertionError("Should have given NotFoundException");
    }

    @Test
    public void loadMultiple() throws Exception {
        MultipleMediaResult o = clients.getMediaService().loadMultiple(IdList.of(mids), null, null);

        for (int i = 0; i < mids.size(); i++) {
            assertThat(o.getItems().get(i).getResult().getMid()).isEqualTo(mids.get(i));
            if (profileName != null) {
                assertThat(profile.getMediaProfile().test(o.getItems().get(i).getResult())).isTrue();
            }
        }

    }



}