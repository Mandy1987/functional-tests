package nl.vpro.testutils;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.security.Permission;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import javax.management.ObjectName;
import javax.management.remote.*;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;

import com.sun.tools.attach.*;

import nl.vpro.util.CommandExecutor;
import nl.vpro.util.CommandExecutorImpl;

import static java.lang.Integer.parseInt;

/**
 * Support for JMX access on the system that we test. The idea is the start with {@link #getMBeanServerConnection(Map)}
 *
 * This is accessible via a junit 5 extension {@link nl.vpro.junit.extensions.JMXContainers}
 *
 * @author Michiel Meeuwissen
 */
@Log4j2
public class JMXSupport {

    @SneakyThrows
    public static ObjectName getObjectName(String objectName) {
         return new ObjectName(objectName);
    }


    /**
     * Returns a {@link JMXContainer} with the necessary object to access JMX for a server that is being tested.
     *
     * Provided it with property like so:
     * {@code
     *   JMXSupport.getMBeanServerConnection(getCONFIG.getProperties(Config.Prefix.poms)));
     * }
     *
     * Most of the time JMX is not accessible for the world, and this has to happen via an SSH tunnel.
     *
     * This method also arranges that (if the 'ssh-host' property is found).
     *
     */
    @SneakyThrows
    public static JMXContainer getMBeanServerConnection(Map<String, String> properties) {
        String ssh = properties.get("ssh-host");
        String host = properties.get("jmx-host");
        final JMXContainer container = new JMXContainer();


        String jmxUrl = properties.get("jmx-url");
        String servicePort = properties.get("jmx-service-port");
        String  jndiPort  = properties.get("jmx-jndi-port");
        if (jmxUrl == null) {
            jmxUrl = String.format("service:jmx:rmi://%s:%d/jndi/rmi://%s:%d/jmxrmi", host, parseInt(servicePort), host, parseInt(jndiPort));
            log.info("Constructed jmx url: {}", jmxUrl);
        } else {
            log.info("Found jmx url: {}", jmxUrl);
        }
        if (StringUtils.isNotBlank(ssh)) {
            createTunnel(container, host, ssh, parseInt(servicePort), parseInt(jndiPort));
        }
        String userName = properties.get("jmx-username");
        String password = properties.get("jmx-password");

        createMBeanServerConnection(container, userName, password, jmxUrl);

        return container;
    }

    protected static void createTunnel(JMXContainer container, String host, String ssh, int... ports) throws InterruptedException {

        CommandExecutorImpl tunnel = CommandExecutorImpl.builder()
            .executablesPaths("/usr/bin/ssh")
            .build();

        IntConsumer ready = (i) -> log.info("Finished {}", i);

        Consumer<Process> consumer = (p) -> {
            synchronized (container) {
                container.pid = p.pid();
                container.notifyAll();
            }
        };
        CommandExecutor.Parameters.Builder builder = CommandExecutor.Parameters.builder()
            .consumer(consumer);


        for (int p : ports) {
            builder.arg("-L", p + ":" + host + ":" + p);
        }
        builder.arg(ssh);
        builder.submit(ready, tunnel);


        log.info("Waiting for pid after sumitting " + builder.toString());
        synchronized (container) {
            while (container.pid == 0) {
                container.wait();
            }
        }
        Thread.sleep(1000);
    }

    protected static void createMBeanServerConnection(
        JMXContainer container, String userName, String password,
        String jmxUrl) throws IOException {


          System.setSecurityManager(new SecurityManager() {
            @Override
            public void checkPermission(Permission perm) {

            }
        });
        JMXServiceURL url = jmxUrl.startsWith("localhost:") ? localhost(jmxUrl.substring("localhost:".length())) : new JMXServiceURL(jmxUrl);
        Map<String, Object> env = new HashMap<>();
        String[] credentials = { userName, password };

        env.put(JMXConnector.CREDENTIALS, credentials);
        container.connector = JMXConnectorFactory.connect(url, env);
        container.connector.connect();

        container.connection = container.connector.getMBeanServerConnection();


    }

    @AfterAll
    public static void killTunnel(long pid) {
        if (pid > 0) {
            log.info("Killing {}", pid);
            CommandExecutorImpl.builder()
                .executablesPaths("/bin/kill")
                .build()
                .execute(String.valueOf(pid));
        }

    }

    public static JMXServiceURL localhost(String pid) throws IOException {
        List<VirtualMachineDescriptor> vms = VirtualMachine.list();
        for (VirtualMachineDescriptor desc : vms) {
            VirtualMachine vm;
            try {
                vm = VirtualMachine.attach(desc);
            } catch (AttachNotSupportedException e) {
                log.info("Attach not supported for {}", desc);
                continue;
            } catch (IOException ioe) {
                log.info("IO {}", desc);
                continue;
            }
            if (!vm.id().equals(pid)) {
                continue;
            }
            Properties props = vm.getAgentProperties();
            String connectorAddress =
                props.getProperty("com.sun.management.jmxremote.localConnectorAddress");
            if (connectorAddress == null) {
                continue;
            }
            JMXServiceURL url = new JMXServiceURL(connectorAddress);
            return url;
        }
        return null;
    }

}
