/* vim: set et ts=2 sw=2 cindent fo=qroca: */

package com.k2.hibernate;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitJoinTableNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl;

/** A naming strategy that uses full composite paths extracted from
 * AttributePath, as opposed to just the terminal property part.
 */
public class K2DbImplicitNamingStrategyComponentPath
    extends K2DbImplicitNamingStrategy {

  /** Constructor, creates a new naming strategy.
   */
  K2DbImplicitNamingStrategyComponentPath() {
    super(new ImplicitNamingStrategyComponentPathImpl());
  }

  @Override
  public Identifier determineJoinTableName(
      final ImplicitJoinTableNameSource source) {
    String name = source.getOwningPhysicalTableName() + "_"
      + source.getAssociationOwningAttributePath().getProperty();
    return apply(toIdentifier(name, source.getBuildingContext()));
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

