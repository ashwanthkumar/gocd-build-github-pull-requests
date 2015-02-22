package in.ashwanthkumar.gocd.github;

import in.ashwanthkumar.gocd.github.model.Revision;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class JGitHelperTest {
    private static final int BUFFER_SIZE = 4096;

    private JGitHelper jGit = new JGitHelper();
    private File testRepository = new File("/tmp", UUID.randomUUID().toString());
    private File simpleGitRepository = new File("/tmp", "simple-git-repository");

    @Before
    public void setUp() {
        cleanTmpFiles();
    }

    @After
    public void tearDown() {
        cleanTmpFiles();
    }

    private void cleanTmpFiles() {
        FileUtils.deleteQuietly(testRepository);
        FileUtils.deleteQuietly(simpleGitRepository);
    }

    @Test
    public void shouldPollRepositoryCorrectly() throws Exception {
        // Checkout & Get LatestRevision
        extractToTmp("/sample-repository/simple-git-repository-1.zip");

        jGit.cloneOrFetch(simpleGitRepository.getAbsolutePath(), testRepository.getAbsolutePath());

        Revision revision = jGit.getLatestRevision(testRepository.getAbsolutePath());

        verifyRevision(revision, "012e893acea10b140688d11beaa728e8c60bd9f6", "1", asList(new Pair("a.txt", "added")));

        // Fetch & Get LatestRevisionsSince
        FileUtils.deleteQuietly(simpleGitRepository.getAbsoluteFile());
        extractToTmp("/sample-repository/simple-git-repository-2.zip");

        jGit.cloneOrFetch(simpleGitRepository.getAbsolutePath(), testRepository.getAbsolutePath());

        List<Revision> newerRevisions = jGit.getNewerRevisions(testRepository.getAbsolutePath(), "012e893acea10b140688d11beaa728e8c60bd9f6");

        assertThat(newerRevisions.size(), is(2));
        verifyRevision(newerRevisions.get(0), "24ce45d1a1427b643ae859777417bbc9f0d7cec8", "3\ntest multiline\ncomment", asList(new Pair("a.txt", "modified"), new Pair("b.txt", "added")));
        verifyRevision(newerRevisions.get(1), "1320a78055558603a2c29d803bbaa50d3542ff50", "2", asList(new Pair("a.txt", "modified")));
    }

    @Test
    public void shouldCheckoutToRevision() throws Exception {
        extractToTmp("/sample-repository/simple-git-repository-2.zip");

        jGit.checkoutToRevision(simpleGitRepository.getAbsolutePath(), "24ce45d1a1427b643ae859777417bbc9f0d7cec8");

        assertThat(new File(simpleGitRepository, "a.txt").exists(), is(true));
        assertThat(new File(simpleGitRepository, "b.txt").exists(), is(true));

        jGit.checkoutToRevision(simpleGitRepository.getAbsolutePath(), "1320a78055558603a2c29d803bbaa50d3542ff50");

        assertThat(new File(simpleGitRepository, "a.txt").exists(), is(true));
        assertThat(new File(simpleGitRepository, "b.txt").exists(), is(false));
    }

    private void extractToTmp(String zipResourcePath) throws IOException {
        File zipFile = new File("/tmp", UUID.randomUUID().toString() + ".zip");

        IOUtils.copy(getClass().getResourceAsStream(zipResourcePath), new FileOutputStream(zipFile));

        unzip(zipFile.getAbsolutePath(), "/tmp");

        FileUtils.deleteQuietly(zipFile);
    }

    private void unzip(String zipFilePath, String destinationDirectoryPath) throws IOException {
        File destinationDirectory = new File(destinationDirectoryPath);
        if (!destinationDirectory.exists()) {
            FileUtils.forceMkdir(destinationDirectory);
        }

        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipInputStream.getNextEntry();
        while (entry != null) {
            String filePath = destinationDirectoryPath + File.separator + entry.getName();
            if (!entry.isDirectory()) {
                extractFile(zipInputStream, filePath);
            } else {
                FileUtils.forceMkdir(new File(filePath));
            }

            zipInputStream.closeEntry();
            entry = zipInputStream.getNextEntry();
        }
        zipInputStream.close();
    }

    private void extractFile(ZipInputStream zipInputStream, String filePath) throws IOException {
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesRead = new byte[BUFFER_SIZE];
        int readByteCount = 0;
        while ((readByteCount = zipInputStream.read(bytesRead)) != -1) {
            bufferedOutputStream.write(bytesRead, 0, readByteCount);
        }
        bufferedOutputStream.close();
    }

    private void verifyRevision(Revision revision, String sha, String comment, List<Pair> files) {
        assertThat(revision.getRevision(), is(sha));
        assertThat(revision.getComment(), is(comment));
        assertThat(revision.getModifiedFiles().size(), is(files.size()));
        for (int i = 0; i < files.size(); i++) {
            assertThat(revision.getModifiedFiles().get(i).getFileName(), is(files.get(i).a));
            assertThat(revision.getModifiedFiles().get(i).getAction(), is(files.get(i).b));
        }
    }

    class Pair {
        String a;
        String b;

        public Pair(String a, String b) {
            this.a = a;
            this.b = b;
        }
    }
}