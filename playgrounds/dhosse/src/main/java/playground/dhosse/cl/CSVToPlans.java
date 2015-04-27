package playground.dhosse.cl;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

public class CSVToPlans {
	
	private final String filename;
	
	private final int idxPersonId = 1;
	private final int idxOriginX = 10;
	private final int idxOriginY = 11;
	private final int idxDestinationX = 12;
	private final int idxDestinationY = 13;
	private final int idxActType = 14;
	private final int idxModes = 17;
	private final int idxStartTime = 21;
	private final int idxEndTime = 22;

	/*
	 * proposito
	 * 1 al trabajo = zur Arbeit
	 * 2 por trabajo = geschäftlich
	 * 3 al estudio = zur Ausbildung
	 * 4 por estudio = wg. d. Ausbildung
	 * 5 de salud = Gesundheit (evtl. Arzt)
	 * 6 visitar al alguien = jmd. besuchen
	 * 7 volver a casa = nach Hause
	 * 8 buscar o dejar a alguien
	 * 9 comer o tomar algo = essen & trinken
	 * 10 buscar o dejar algo
	 * 11 de compras = Einkaufen
	 * 12 tramites = Formalitäten
	 * 13 recreacion = Erholung
	 * 14 otra actividados = andere...
	 */
	//actividad destino
	//work
	private final int actTypeIndustria = 1;
	private final int actTypeComercio = 2;
	private final int actTypeSalud = 3;
	private final int actTypeServicios = 5;
	private final int actTypeSectorPublico = 7;
	//education
	private final int actTypeEducacion = 4;
	//home
	private final int actTypeHabitacional = 6;
	//other
	private final int actTypeOther = 8;
	
	//modo
	//car
	private final int modeAutoChofer = 1; //Auto als Fahrer
	private final int modeAutoAcompanante = 17; //Auto Beifahrer
	//walk
	private final int modeEntramenteAPie = 8; //Fuß
	//pt
	private final int modeBusAlimentador = 2; //Feederbus
	private final int modeBusTroncal = 3; //Bus (Stammstrecke)
	private final int modeMetro = 4; //U-Bahn
	private final int modeFurgonEscolarComoPasajero = 6; //Schulbus als Passagier
	private final int modeBusInstitucional = 11; //
	private final int modeBusInterurbanoORural = 12; //Überlandbus
	private final int modeFurgonEscolarComoChoferOAcompanante = 13; //Schulbus als Fahrer oder Begleiter
	private final int modeBusUrbanoConPagoAlConductor = 14; //Metrobus
	private final int modeTren = 16; //Zug
	//taxi
	private final int modeTaxiColectivo = 5; //Sammeltaxi
	private final int modeTaxiORadioTaxi = 7; //Taxi oder Ruftaxi
	//bike
	private final int modeBicicleta = 9; //Fahrrad
	//ride
	private final int modeMotocicleta = 10; //Motorrad
	private final int modeServicioInformal = 15; //
	private final int modeMotocicletaAcompanante = 18; //Motorrad Beifahrer
	

	public CSVToPlans(String filename){
		
		this.filename = filename;
		
	}
	
	public void run(String outputFile){
		
		BufferedReader reader = IOUtils.getBufferedReader(this.filename);
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		//Network
		Population population = scenario.getPopulation();
		PopulationFactoryImpl popFactory = (PopulationFactoryImpl) population.getFactory();
		
		Map<Id<Person>, Person> personsMap = new TreeMap<Id<Person>, Person>();
		
		try {
			
			String line = reader.readLine();
			
			while((line = reader.readLine()) != null){
				
				String[] splittedLine = line.split(";");
				
				Id<Person> personId = Id.createPersonId(splittedLine[this.idxPersonId]);
				
				if(!personsMap.containsKey(personId)){
					
					Person person = popFactory.createPerson(personId);
					Plan plan = popFactory.createPlan();
					
					Coord coord = new CoordImpl(splittedLine[this.idxOriginX], splittedLine[this.idxOriginY]);
					Activity act1 = popFactory.createActivityFromCoord("h", coord);
					
					double endTime = Time.parseTime(splittedLine[this.idxStartTime]);
					act1.setEndTime(endTime);
					
					Coord coord2 = new CoordImpl(splittedLine[this.idxDestinationX], splittedLine[this.idxDestinationY]);
					String actTypeIdx = splittedLine[this.idxActType];
					String actType = !actTypeIdx.equals("") ? this.getActType(Integer.parseInt(actTypeIdx)) : "o";
					Activity act2 = popFactory.createActivityFromCoord(actType, coord2);
					
					double startTimeAct2 = Time.parseTime(splittedLine[this.idxEndTime]);
					act2.setStartTime(startTimeAct2);
					
					Set<Leg> legs = new HashSet<Leg>();
					String[] modes = splittedLine[this.idxModes].split("_");
					String lastMode = null;
					
					for(int i = 0; i < modes.length; i++){
						
						Leg leg = popFactory.createLeg(this.getLegMode(Integer.parseInt(modes[i])));
						
						if(lastMode != null){
							
							if(lastMode.equals(leg.getMode())){
								
								continue;
								
							}
							
						}
						
						legs.add(leg);
						lastMode = leg.getMode();
						
					}
					
					plan.addActivity(act1);
					
					for(Leg leg : legs){
						plan.addLeg(leg);
					}
					plan.addActivity(act2);
					
					person.addPlan(plan);
					
					person.setSelectedPlan(plan);
					
					personsMap.put(personId, person);
					
				} else {
					
					Person person = personsMap.get(personId);
					Plan selectedPlan = person.getSelectedPlan();
					
					Activity lastActivity = (Activity) selectedPlan.getPlanElements().get(selectedPlan.getPlanElements().size()-1);
					
					double endTime = Time.parseTime(splittedLine[this.idxStartTime]);
					lastActivity.setEndTime(endTime);
					
					Coord coord = new CoordImpl(splittedLine[this.idxDestinationX], splittedLine[this.idxDestinationY]);
					String actTypeIdx = splittedLine[this.idxActType];
					String actType = !actTypeIdx.equals("") ? this.getActType(Integer.parseInt(actTypeIdx)) : "o";
					Activity act2 = popFactory.createActivityFromCoord(actType, coord);
					
					double startTime = Time.parseTime(splittedLine[this.idxEndTime]);
					act2.setStartTime(startTime);
					
					Set<Leg> legs = new HashSet<Leg>();
					String[] modes = splittedLine[this.idxModes].split("_");
					for(int i = 0; i < modes.length; i++){
						Leg leg = popFactory.createLeg(this.getLegMode(Integer.parseInt(modes[i])));
						legs.add(leg);
					}
					
					String lastMode = null;
					for(Leg leg : legs){
						
						if(lastMode != null){
							if(lastMode.equalsIgnoreCase(leg.getMode())){
								continue;
							}
						}
						lastMode = leg.getMode();
						selectedPlan.addLeg(leg);
						
					}
					
					selectedPlan.addActivity(act2);
					
				}
			
			}
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
		for(Person person : personsMap.values()){
			population.addPerson(person);
		}
		
		new PopulationWriter(population).write(outputFile);
		
	}
	
	private String getActType(int index){
		
		switch(index){
			case 1:
			case 2: return "w";
			case 3:
			case 4: return "e";
			case 7: return "h";
			case 11: return "s";
			case 13: return "l";
			case 5:
			case 6: 
			case 8:
			case 9:
			case 10:
			case 12:
			case 14:
			default: return "o"; //not specified
		}
		
	}
	
	private String getLegMode(int index){
		
		switch(index){
			case 1:
			case 17: return TransportMode.car;
			case 8: return TransportMode.walk;
			case 2:
			case 3:
			case 4:
			case 6:
			case 11:
			case 12:
			case 13:
			case 14:
			case 16: return TransportMode.pt;
			case 5:
			case 7: return "taxi";
			case 9: return TransportMode.bike;
			case 10:
			case 15:
			case 18: return TransportMode.ride;
			default: return null;
		}
		
	}
	
	class PersonTemplate{
		
		//pattern means sequence of activities and legs the person performs during the day
		//formatted like this:
		// actType_coordX-coordY_startTime-endTime===legMode===actType_...
		private StringBuffer pattern;
		
		public PersonTemplate(StringBuffer activityAndTravelPattern){
			this.pattern = activityAndTravelPattern;
		}
		
		public StringBuffer getPattern(){
			return this.pattern;
		}
		
	}
	
}
