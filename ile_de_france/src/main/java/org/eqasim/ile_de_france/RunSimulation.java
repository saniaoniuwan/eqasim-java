package org.eqasim.ile_de_france;

import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), Configurator.getConfigGroups());
		cmd.applyConfiguration(config);

		Scenario scenario = ScenarioUtils.createScenario(config);
		Configurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		Configurator.adjustScenario(scenario);

		Controler controller = new Controler(scenario);
		Configurator.configureController(controller);
		controller.addOverridingModule(new IDFModeChoiceModule(cmd));
		controller.run();
	}
}