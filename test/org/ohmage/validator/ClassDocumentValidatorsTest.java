/*******************************************************************************
 * Copyright 2012 The Regents of the University of California
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohmage.validator;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;
import org.ohmage.exception.ValidationException;
import org.ohmage.test.ParameterSets;

/**
 * Tests the class-document validators.
 * 
 * @author John Jenkins
 */
public class ClassDocumentValidatorsTest extends TestCase {
	/**
	 * Tests the class ID, document role validator.
	 */
	@Test
	public void testValidateClassIdAndDocumentRoleList() {
		try {
			for(String emptyValue : ParameterSets.getEmptyValues()) {
				Assert.assertNull(ClassDocumentValidators.validateClassIdAndDocumentRoleList(emptyValue));
			}
			
			try {
				ClassDocumentValidators.validateClassIdAndDocumentRoleList("Invalid value.");
				fail("The campaign ID, document role list was an invalid value.");
			}
			catch(ValidationException e) { 
				// Passed.
			}
			
			for(String validUrnDocumentRoleList : ParameterSets.getValidUrnDocumentRoleLists()) {
				try {
					ClassDocumentValidators.validateClassIdAndDocumentRoleList(validUrnDocumentRoleList);
				}
				catch(ValidationException e) {
					fail("A valid URN, campaign role list failed validation: " + validUrnDocumentRoleList);
				}
			}
			
			for(String invalidUrnDocumentRoleList : ParameterSets.getInvalidUrnDocumentRoleLists()) {
				try {
					ClassDocumentValidators.validateClassIdAndDocumentRoleList(invalidUrnDocumentRoleList);
					fail("An invalid URN, campaign role list passed validation: " + invalidUrnDocumentRoleList);
				}
				catch(ValidationException e) {
					// Passed.
				}
			}
		}
		catch(ValidationException e) {
			fail("A validation exception was thrown: " + e.getMessage());
		}
	}
}
