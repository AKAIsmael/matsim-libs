package playground.vsp.bvwp2;


import static playground.vsp.bvwp2.Key.*;



public class MultiDimensionalArray {
	/**
	 * Design thoughts:<ul>
	 * <li> yyyy rename to "ODAttribute".  "Mengen" sind vielleicht nicht wirklich "Attribute" im Sinne der BVWP-Nomenklatur,
	 * aber abgesehen davon spricht eigentlich nichts dagegen.  kai,benjamin, sep'12
	 * </ul>
	 */
	enum Attribute { XX, km, hrs, priceUser, costOfProduction, access_hrs }

	/**
	 * Design thoughts:<ul>
	 * <li> yyyy rename to "DemandSegment".  But refactoring does not seem to work.  Try with other eclipse (this one was
	 * Galileo).  kai/benjamin, sep'12
	 *</ul>
	 */
	enum DemandSegment { GV, PV_NON_COMMERCIAL, PV_COMMERCIAL }

	enum Mode { ROAD, RAIL }
	
	public static void main(String[] args) {
		Values m = new Values() ;
		
		double sum = 0 ;

		for ( DemandSegment segm : DemandSegment.values() ) {
			for ( Mode mode : Mode.values() ) {
				sum += m.get(makeKey(mode,segm, Attribute.km )) ;
			}
		}
		
		
		
		
	}
	
}
