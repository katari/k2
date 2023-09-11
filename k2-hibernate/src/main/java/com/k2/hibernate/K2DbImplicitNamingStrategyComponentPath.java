/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import java.lang.reflect.Field;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitBasicColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitEntityNameSource;
import org.hibernate.boot.model.naming.ImplicitJoinTableNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.spi.MetadataBuildingContext;

/** A naming strategy that uses full composite paths extracted from
 * AttributePath, as opposed to just the terminal property part.
 *
 * This naming strategy supports the annotation @Prefix to customize the prefix
 * to use for the embedded column names.
 */
public class K2DbImplicitNamingStrategyComponentPath
    extends K2DbImplicitNamingStrategy {

  /** The current entity class being processed.
   *
   * This is a hack: we are depending on an implementation detail of the
   * client: determinePrimaryTableName will be called first, then all the
   * necessary determineBasicColumnName for the same class, and this class is
   * used in a single thread context.
   */
  private Class<?> currentClass = null;

  /** Constructor, creates a new naming strategy.
   */
  K2DbImplicitNamingStrategyComponentPath() {
    super(new ImplicitNamingStrategyComponentPathImpl());
  }

  @Override
  public Identifier determinePrimaryTableName(
      final ImplicitEntityNameSource source) {
    try {
      currentClass = Class.forName(source.getEntityNaming().getClassName());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Error", e);
    }

    return apply(super.determinePrimaryTableName(source));
  }

  @Override
  public Identifier determineJoinTableName(
      final ImplicitJoinTableNameSource source) {
    String name = source.getOwningPhysicalTableName() + "_"
      + source.getAssociationOwningAttributePath().getProperty();
    return apply(toIdentifier(name, source.getBuildingContext()));
  }

  @Override
  public Identifier determineBasicColumnName(
      final ImplicitBasicColumnNameSource source) {

    Pair<Boolean, String> prefix = getPrefix(source.getAttributePath());

    String pathAsString = source.getAttributePath().getFullPath();

    if (prefix.getLeft() || !prefix.getRight().equals("")) {
      // We are asked to remove the column name prefix or replace it by a
      // custom one. Strip the first component from the full path.
      pathAsString = pathAsString.replaceAll("^[^.]*\\.", "");
    }

    if (!prefix.getRight().equals("")) {
      // Add the new prefix to the path.
      pathAsString = prefix.getRight() + "." + pathAsString;
    }

    final AttributePath path = AttributePath.parse(pathAsString);
    Identifier result = apply(super.determineBasicColumnName(
        new ImplicitBasicColumnNameSource() {
          @Override
          public MetadataBuildingContext getBuildingContext() {
            return source.getBuildingContext();
          }
          @Override
          public AttributePath getAttributePath() {
            return path;
          }
          @Override
          public boolean isCollectionElement() {
            return source.isCollectionElement();
          }
        }));

    return result;
  }

  /** Returns the values of the prefix annotation from the the root attribute
   * in source.
   *
   * @param attributePath the attribute path from the top level entity. It
   * cannot be null.
   *
   * @return a pair where the first element is true to skip the prefix from
   * the embeddable column name, and a string with the value of the prefix.
   * Never returns null.
   */
  private Pair<Boolean, String> getPrefix(final AttributePath attributePath) {

    AttributePath rootAttribute = attributePath;
    while (!rootAttribute.getParent().isRoot()) {
      rootAttribute = rootAttribute.getParent();
    }

    Field[] fields = currentClass.getDeclaredFields();
    Field columnField = null;
    for (Field field : fields) {
      if (field.getName().equals(rootAttribute.getProperty())) {
        columnField = field;

        Prefix prefix = columnField.getAnnotation(Prefix.class);
        if (prefix != null) {
          if (prefix.skip()) {
            Validate.isTrue(prefix.value().equals(""),
              "You cannot specify a prefix value when skipping the prefix");
          }
          return Pair.of(prefix.skip(), prefix.value());
        } else {
          return Pair.of(false, "");
        }
      }
    }
    return Pair.of(false, "");
  }

  /** Removes the collection_&&_element_ string in the name that appears due to
   * ImplicitNamingStrategyComponentPathImpl.
   *
   * Related to HHH-6005.
   */
  @Override
  protected Identifier apply(final Identifier name) {
    String nameText = super.apply(name).getText();

    if (nameText.contains("collection_&&_element_")) {
      // This should not be necessary (see HHH-6005), but for some reason, the
      // parent expects the dot instead of the '_'.
      nameText = nameText.replace("collection_&&_element_", "");
    }
    return new Identifier(nameText, name.isQuoted());
  }
}

