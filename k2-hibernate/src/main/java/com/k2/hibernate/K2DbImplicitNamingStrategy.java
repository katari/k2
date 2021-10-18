/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitAnyDiscriminatorColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitAnyKeyColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitBasicColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitCollectionTableNameSource;
import org.hibernate.boot.model.naming.ImplicitDiscriminatorColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitEntityNameSource;
import org.hibernate.boot.model.naming.ImplicitForeignKeyNameSource;
import org.hibernate.boot.model.naming.ImplicitIdentifierColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitIndexColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitIndexNameSource;
import org.hibernate.boot.model.naming.ImplicitJoinColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitJoinTableNameSource;
import org.hibernate.boot.model.naming.ImplicitMapKeyColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitPrimaryKeyJoinColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitTenantIdColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitUniqueKeyNameSource;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;

/** A k2 provided implicit naming strategy that mainly converts java camel case
 * to underscore separated lower case.
 *
 * It is based on the jpa naming strategy, slightly modified by spring for
 * many to many relations.
 *
 * Note: there are two mechanisms to define a unique key: using the unique=true
 * in some annotations (like @Column and @JoinColumn) or with a @UniqueContraint
 * annotation in an @Table declaration. Due to bug
 * https://hibernate.atlassian.net/browse/HHH-11103, when using unique=true,
 * the generated unique index name will be something like UX_234234234, instead
 * of a human readable one.
 *
 * This naming strategy may generate very large names, so it may not be usable
 * for certain databases (oracle for instance).
 */
public class K2DbImplicitNamingStrategy implements ImplicitNamingStrategy {

  /** The base implementation of this naming strategy, never null. */
  private ImplicitNamingStrategy delegate;

  /** Constructor, creates a new naming strategy.
   */
  K2DbImplicitNamingStrategy() {
    delegate = new SpringImplicitNamingStrategy();
  }

  /** Constructor, creates a new naming strategy, based on the specified one.
   *
   * @param base the naming strategy on which this one is based. It cannot be
   * null.
   */
  K2DbImplicitNamingStrategy(final ImplicitNamingStrategy base) {
    delegate = base;
  }

  @Override
  public Identifier determinePrimaryTableName(
      final ImplicitEntityNameSource source) {
    return apply(delegate.determinePrimaryTableName(source));
  }

  @Override
  public Identifier determineJoinTableName(
      final ImplicitJoinTableNameSource source) {
    return apply(delegate.determineJoinTableName(source));
  }

  /** Easy hook to build an Identifier using the keyword safe IdentifierHelper.
   *
   * @param stringForm The String form of the name
   *
   * @param buildingContext Access to the IdentifierHelper
   *
   * @return The identifier
   */
  Identifier toIdentifier(final String stringForm,
      final  MetadataBuildingContext buildingContext) {
    return buildingContext.getMetadataCollector()
        .getDatabase()
        .getJdbcEnvironment()
        .getIdentifierHelper()
        .toIdentifier(stringForm);
  }

  @Override
  public Identifier determineCollectionTableName(
      final ImplicitCollectionTableNameSource source) {
    return apply(delegate.determineCollectionTableName(source));
  }

  @Override
  public Identifier determineDiscriminatorColumnName(
      final ImplicitDiscriminatorColumnNameSource source) {
    return apply(delegate.determineDiscriminatorColumnName(source));
  }

  @Override
  public Identifier determineTenantIdColumnName(
      final ImplicitTenantIdColumnNameSource source) {
    return apply(delegate.determineTenantIdColumnName(source));
  }

  @Override
  public Identifier determineIdentifierColumnName(
      final ImplicitIdentifierColumnNameSource source) {
    return apply(delegate.determineIdentifierColumnName(source));
  }

  @Override
  public Identifier determineBasicColumnName(
      final ImplicitBasicColumnNameSource source) {
    return apply(delegate.determineBasicColumnName(source));
  }

  @Override
  public Identifier determineJoinColumnName(
      final ImplicitJoinColumnNameSource source) {
    return apply(delegate.determineJoinColumnName(source));
  }

  @Override
  public Identifier determinePrimaryKeyJoinColumnName(
      final ImplicitPrimaryKeyJoinColumnNameSource source) {
    return apply(delegate.determinePrimaryKeyJoinColumnName(source));
  }

  @Override
  public Identifier determineAnyDiscriminatorColumnName(
      final ImplicitAnyDiscriminatorColumnNameSource source) {
    return apply(delegate.determineAnyDiscriminatorColumnName(source));
  }

  @Override
  public Identifier determineAnyKeyColumnName(
      final ImplicitAnyKeyColumnNameSource source) {
    return apply(delegate.determineAnyKeyColumnName(source));
  }

  @Override
  public Identifier determineMapKeyColumnName(
      final ImplicitMapKeyColumnNameSource source) {
    return apply(delegate.determineMapKeyColumnName(source));
  }

  @Override
  public Identifier determineListIndexColumnName(
      final ImplicitIndexColumnNameSource source) {
    return apply(delegate.determineListIndexColumnName(source));
  }

  /** Creates a foreign key name concatenating "fk", the table name and the
   * list of columns of the table in alphabetical order, all separated by '_'.
   *
   * It uses the user provided identifier, if any.
   */
  @Override
  public Identifier determineForeignKeyName(
      final ImplicitForeignKeyNameSource source) {

    Identifier result = source.getUserProvidedIdentifier();
    if (result == null) {
      StringBuilder fkName = new StringBuilder();

      fkName.append("fk_");
      fkName.append(source.getTableName().getText());
      for (Identifier columnName : sort(source.getColumnNames())) {
        fkName.append("_").append(columnName.getText());
      }
      result = apply(source.getBuildingContext().getObjectNameNormalizer()
          .normalizeIdentifierQuoting(toIdentifier(fkName.toString(),
              source.getBuildingContext())));
    }
    return result;
  }

  /** Creates a unique key name concatenating "uk" and the list of columns of
   * the table in alphabetical order, separated by '_'.
   */
  @Override
  public Identifier determineUniqueKeyName(
      final ImplicitUniqueKeyNameSource source) {
    StringBuilder fkName = new StringBuilder();
    fkName.append("uk_");
    fkName.append(source.getTableName());
    for (Identifier columnName : source.getColumnNames()) {
      fkName.append("_").append(columnName.getText());
    }
    return apply(source.getBuildingContext().getObjectNameNormalizer()
        .normalizeIdentifierQuoting(toIdentifier(fkName.toString(),
            source.getBuildingContext())));
  }

  /** Creates an index name concatenating "idx" and the list of columns of the
   * table in alphabetical order, separated by '_'.
   */
  @Override
  public Identifier determineIndexName(final ImplicitIndexNameSource source) {
    StringBuilder fkName = new StringBuilder();
    fkName.append("idx");

    for (Identifier columnName : source.getColumnNames()) {
      fkName.append("_").append(columnName.getText());
    }

    return apply(source.getBuildingContext().getObjectNameNormalizer()
        .normalizeIdentifierQuoting(toIdentifier(fkName.toString(),
            source.getBuildingContext())));
  }

  /** Alphabetically sorts a list of identifiers.
   *
   * @param identifiers the list of identifiers to sort. It cannot be null.
   *
   * @return a new arry with the identifiers alphabetically sorted. Never
   * returns null.
   */
  private Identifier[] sort(final List<Identifier> identifiers) {
    Identifier[] result;
    result = identifiers.toArray(new Identifier[identifiers.size()]);
    Arrays.sort(
        result,
        new Comparator<Identifier>() {
          @Override
          public int compare(final Identifier o1, final Identifier o2) {
            return o1.getCanonicalName().compareTo(o2.getCanonicalName());
          }
        }
    );
    return result;
  }

  /** Applies the name transformation to the provided name.
   *
   * We consider that each identifier is a concatenation of words and numbers,
   * in camel case. Each word begins with an upper case letter and is followed
   * by at least one lower case letter.
   *
   * This operation determines if the current character starts a word or number
   * based on the character just before it.
   *
   * Examples of transformations are:
   *
   * a11 -> a_11
   *
   * aBa -> a_ba
   *
   * aB ->a_b (notice that in this case, B is considered a word anyways).
   *
   * imageUrl -> image_url
   *
   * imageURL -> image_url.
   *
   * URLForImage -> url_for_image.
   *
   * @param name the name to transform. It cannot be null.
   *
   * @return a name converted form camel case to underscore separated words. It
   * never returns null.
   */
  protected Identifier apply(final Identifier name) {
    LinkedList<String> result = new LinkedList<>();
    String nameText = name.getText();
    for (String part : StringUtils.splitByCharacterTypeCamelCase(nameText)) {
      // Remove all underscores.
      part = part.replace("_", "");
      if (part == null || part.trim().isEmpty()) {
        // skip null, space.
        continue;
      }
      result.add(part.toLowerCase(Locale.ROOT));
    }
    return new Identifier(StringUtils.join(result, "_"), name.isQuoted());
  }
}

