package de.mpg.imeji.logic.validation;

import de.mpg.imeji.logic.validation.impl.AlbumValidator;
import de.mpg.imeji.logic.validation.impl.CollectionValidator;
import de.mpg.imeji.logic.validation.impl.ItemValidator;
import de.mpg.imeji.logic.validation.impl.MetadataValidator;
import de.mpg.imeji.logic.validation.impl.ProfileValidator;
import de.mpg.imeji.logic.validation.impl.PseudoValidator;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.MetadataProfile;

/**
 * Factory for {@link Validator}
 * 
 * @author saquet
 *
 */
public class ValidatorFactory {
	/**
	 * Return a new {@link Validator} according to the object class
	 * 
	 * @param <T>
	 * 
	 * @param t
	 * @return
	 */
	public static Validator<?> newValidator(Object obj) {
		if (obj instanceof Item) {
			return new ItemValidator();
		} else if (obj instanceof Metadata) {
			return new MetadataValidator();
		} else if (obj instanceof CollectionImeji) {
			return new CollectionValidator();
		} else if (obj instanceof Album) {
			return new AlbumValidator();
		} else if (obj instanceof MetadataProfile) {
			return new ProfileValidator();
		}
		return new PseudoValidator();
	}
}