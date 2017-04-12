package com.facebook.buck.android.aapt;

import com.facebook.buck.log.Logger;
import com.facebook.buck.rules.SourcePath;
import com.facebook.buck.rules.SourcePathResolver;
import com.facebook.buck.step.ExecutionContext;
import com.facebook.buck.step.Step;
import com.facebook.buck.step.StepExecutionResult;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import android.databinding.tool.DataBindingBuilder;
import android.databinding.tool.LayoutXmlProcessor;

public class ProcessDataBindingStep implements Step {

  private static final Logger LOG = Logger.get(ProcessDataBindingStep.class);
  private SourcePathResolver sourcePathResolver;
  private final SourcePath resInDirectory;
  private final Path resOutDirectory;
  private final Path xmlOutDirectory;
  private Optional<Path> sdkDir;
  private final String pkgName;

  public ProcessDataBindingStep(
      SourcePathResolver sourcePathResolver,
      Optional<Path> sdkDir,
      SourcePath resInDirectory,
      Path resOutDirectory,
      Path xmlOutDirectory,
      String pkgName) {
    this.sourcePathResolver = sourcePathResolver;
    this.resInDirectory = resInDirectory;
    this.resOutDirectory = resOutDirectory;
    this.xmlOutDirectory = xmlOutDirectory;
    this.sdkDir = sdkDir;
    this.pkgName = pkgName;
  }

  @Override
  public StepExecutionResult execute(ExecutionContext context) throws IOException, InterruptedException {
    try {
      processDataBindingLayouts();
    } catch (SAXException | XPathExpressionException | JAXBException | ParserConfigurationException e) {
      LOG.error(e.getMessage());
      return StepExecutionResult.ERROR;
    }
    return StepExecutionResult.SUCCESS;
  }

  private void processDataBindingLayouts() throws IOException, XPathExpressionException, SAXException, ParserConfigurationException, JAXBException {
    LayoutXmlProcessor xmlProcessor = new LayoutXmlProcessor(
        pkgName,
        new DataBindingBuilder().createJavaFileWriter(sourcePathResolver.getAbsolutePath(resInDirectory).toFile()),
        19,
        false,
        file -> file
    );

    final LayoutXmlProcessor.ResourceInput resourceInput =
        new LayoutXmlProcessor.ResourceInput(
            false,
            sourcePathResolver.getAbsolutePath(resInDirectory).toFile(),
            resOutDirectory.toFile());
    LOG.info("Processing all res inputs...");
    xmlProcessor.processResources(resourceInput);
    LOG.info("Process Resources, done");

    xmlProcessor.writeLayoutInfoFiles(xmlOutDirectory.toFile());
    xmlProcessor.writeInfoClass(sdkDir.isPresent() ? sdkDir.get().toFile() : null, xmlOutDirectory.toFile(), null);
    LOG.info("Wrote Layout Info files");
  }

  @Override
  public String getShortName() {
    return "process_databinding_info";
  }

  @Override
  public String getDescription(ExecutionContext context) {
    return getShortName() + " " + resInDirectory;
  }
}
