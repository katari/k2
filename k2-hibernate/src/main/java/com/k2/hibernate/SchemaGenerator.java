/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import java.util.EnumSet;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

import org.apache.commons.lang3.Validate;

import org.hibernate.boot.Metadata;

import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

/** The hibernate schema generator.
 *
 * Class to create the ddl from the hibernate module configuration.
 */
public class SchemaGenerator {

  /** The hibernate metadata, properly configured.
   *
   * It cannot be null.
   */
  private Metadata metadata;

  /** SchemaGenerator constructor.
   *
   * @param theMetadata the properly initialized hibernate metadata. It cannot
   * be null.
   */
  public SchemaGenerator(final Metadata theMetadata) {
    Validate.notNull(theMetadata, "The metadata cannot be null.");
    metadata = theMetadata;
  }

  /** Generates the database schema from hibernate metadata.
   *
   * Creates the ddl in the target/schema.ddl directory.
   */
  public void generate() {
    generate("target/schema.ddl");
  }

  /** Generates the database schema from hibernate metadata.
   *
   * @param ddlFile the name of the file where the generator will create
   * with the database schema. It cannot be null.
   */
  public void generate(final String ddlFile) {
    Validate.notNull(ddlFile, "The file name cannot be null.");
    try {
      Files.deleteIfExists(Paths.get(ddlFile));
    } catch (IOException e) {
      throw new RuntimeException("Error deleting " + ddlFile, e);
    }
    SchemaExport schemaExport = new SchemaExport();
    schemaExport.setFormat(true);
    schemaExport.setDelimiter(";");
    schemaExport.setOutputFile(ddlFile);
    schemaExport.execute(EnumSet.of(TargetType.SCRIPT),
        SchemaExport.Action.CREATE, metadata);
  }
}

