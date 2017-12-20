package net.wasdev.wlp.gradle.plugins.tasks.extensions.arquillian;

import org.gradle.api.internal.artifacts.dsl.dependencies.DefaultDependencyHandler
import org.gradle.api.internal.tasks.TaskDependencyInternal
import org.w3c.dom.Element
import net.wasdev.wlp.gradle.plugins.utils.LooseWarApplication
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.apache.commons.io.FileUtils;
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Map.Entry
import javax.xml.parsers.ParserConfigurationException
import javax.xml.xpath.XPathExpressionException
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.impldep.bsh.This
import org.xml.sax.SAXException
import net.wasdev.wlp.common.arquillian.objects.LibertyProperty;
import net.wasdev.wlp.common.arquillian.objects.LibertyRemoteObject
import net.wasdev.wlp.common.arquillian.objects.LibertyManagedObject
import net.wasdev.wlp.common.arquillian.util.ArquillianConfigurationException
import net.wasdev.wlp.common.arquillian.util.Constants
import net.wasdev.wlp.common.arquillian.util.HttpPortUtil
import net.wasdev.wlp.gradle.plugins.tasks.AbstractServerTask
import net.wasdev.wlp.gradle.plugins.tasks.extensions.arquillian.ConfigureArquillianTask.TypeProperty

class ConfigureArquillianTask extends AbstractServerTask {

    public TypeProperty type = TypeProperty.NOTFOUND
    public enum TypeProperty {
        MANAGED, REMOTE, NOTFOUND;
    }
    
    @Input
    public boolean skipIfArquillianXmlExists = false;

    @Input
    public Map<String, String> arquillianProperties = null;

    @TaskAction
    void doExecute() throws GradleException {
        File arquillianXml = new File(project.getBuildDir(), "resources/test/arquillian.xml");
        project.configurations.testCompile.each {

            if(it.toString().contains("arquillian-wlp-remote")) {
                type = TypeProperty.REMOTE;
                return;
            }
            else if(it.toString().contains("arquillian-wlp-managed")) {
                type = TypeProperty.MANAGED
                return;
            }
        }

        if (skipIfArquillianXmlExists && arquillianXml.exists()) {
            logger.info(
                    "Skipping configure-arquillian task because arquillian.xml already exists in \"build/resources/test\".");
        } else {
            if(type == TypeProperty.MANAGED) {
                configureArquillianManaged(arquillianXml);
            }
            if(type == TypeProperty.REMOTE) {
                configureArquillianRemote(arquillianXml);
            }
        }
    }

    private void configureArquillianManaged(File arquillianXml) throws GradleException {
        try {
            LibertyManagedObject arquillianManaged = new LibertyManagedObject(getInstallDir(project).getCanonicalPath(), getServer().name,
                    getHttpPort(), LibertyProperty.getArquillianProperties(arquillianProperties, LibertyManagedObject.LibertyManagedProperty.class));
            arquillianManaged.build(arquillianXml);
        } catch(Exception e) {
            throw new GradleException("Error configuring Arquillian.", e);
        }
    }

    private void configureArquillianRemote(File arquillianXml) throws GradleException {
        try {
            LibertyRemoteObject arquillianRemote = new LibertyRemoteObject(LibertyProperty.getArquillianProperties(arquillianProperties, LibertyRemoteObject.LibertyRemoteProperty.class));
            arquillianRemote.build(arquillianXml);
        } catch(Exception e) {
            throw new GradleException("Error configuring Arquillian.", e);
        }
    }

    /**
     * @return the HTTP port that the managed Liberty server is running on.
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws XPathExpressionException
     * @throws FileNotFoundException
     * @throws ArquillianConfigurationException
     */
    private int getHttpPort() throws FileNotFoundException, XPathExpressionException, IOException, ParserConfigurationException, SAXException, ArquillianConfigurationException {
        String serverDirectory = getServerDir(project);
        File serverXML = new File(serverDirectory + "/server.xml");
        File bootstrapProperties = new File(serverDirectory + "/bootstrap.properties");
        return HttpPortUtil.getHttpPort(serverXML, bootstrapProperties);
    }
}
