package com.varutraproxy.utils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class DomainValidator implements Serializable {
	private static final String DOMAIN_LABEL_REGEX = "\\p{Alnum}(?>[\\p{Alnum}-]*\\p{Alnum})*";
	private static final String TOP_LABEL_REGEX = "\\p{Alpha}{2,}";
	private static final String DOMAIN_NAME_REGEX = "^(?:" + DOMAIN_LABEL_REGEX
			+ "\\.)+" + "(" + TOP_LABEL_REGEX + ")$";

	private static final DomainValidator DOMAIN_VALIDATOR = new DomainValidator();

	private final RegexValidator domainRegex = new RegexValidator(
			DOMAIN_NAME_REGEX);

	public static DomainValidator getInstance() {
		return DOMAIN_VALIDATOR;
	}

	private DomainValidator() {
	}

	public boolean isValid(String domain) {
		String[] groups = domainRegex.match(domain);
		if (groups != null && groups.length > 0) {
			return isValidTld(groups[0]);
		} else {
			return false;
		}
	}

	public boolean isValidTld(String tld) {
		return isValidInfrastructureTld(tld) || isValidGenericTld(tld)
				|| isValidCountryCodeTld(tld);
	}

	public boolean isValidInfrastructureTld(String iTld) {
		return INFRASTRUCTURE_TLD_LIST.contains(chompLeadingDot(iTld
				.toLowerCase()));
	}

	public boolean isValidGenericTld(String gTld) {
		return GENERIC_TLD_LIST.contains(chompLeadingDot(gTld.toLowerCase()));
	}
	public boolean isValidCountryCodeTld(String ccTld) {
		return COUNTRY_CODE_TLD_LIST.contains(chompLeadingDot(ccTld
				.toLowerCase()));
	}

	private String chompLeadingDot(String str) {
		if (str.startsWith(".")) {
			return str.substring(1);
		} else {
			return str;
		}
	}


	private static final String[] INFRASTRUCTURE_TLDS = new String[] { "arpa", 
																				
			"root" 
	};

	private static final String[] GENERIC_TLDS = new String[] { "aero", 
																		
			"asia", // Pan-Asia/Asia Pacific
			"biz", // businesses
			"cat", // Catalan linguistic/cultural community
			"com", // commercial enterprises
			"coop", // cooperative associations
			"info", // informational sites
			"jobs", // Human Resource managers
			"mobi", // mobile products and services
			"museum", // museums, surprisingly enough
			"name", // individuals' sites
			"net", // internet support infrastructure/business
			"org", // noncommercial organizations
			"pro", // credentialed professionals and entities
			"tel", // contact data for businesses and individuals
			"travel", // entities in the travel industry
			"gov", // United States Government
			"edu", // accredited postsecondary US education entities
			"mil", // United States Military
			"int" // organizations established by international treaty
	};

	private static final String[] COUNTRY_CODE_TLDS = new String[] { "ac", // Ascension
																			// Island
			"ad", // Andorra
			"ae", // United Arab Emirates
			"af", // Afghanistan
			"ag", // Antigua and Barbuda
			"ai", // Anguilla
			"al", // Albania
			"am", // Armenia
			"an", // Netherlands Antilles
			"ao", // Angola
			"aq", // Antarctica
			"ar", // Argentina
			"as", // American Samoa
			"at", // Austria
			"au", // Australia (includes Ashmore and Cartier Islands and Coral
					// Sea Islands)
			"aw", // Aruba
			"ax", // è„™éˆ¥î›’and
			"az", // Azerbaijan
			"ba", // Bosnia and Herzegovina
			"bb", // Barbados
			"bd", // Bangladesh
			"be", // Belgium
			"bf", // Burkina Faso
			"bg", // Bulgaria
			"bh", // Bahrain
			"bi", // Burundi
			"bj", // Benin
			"bm", // Bermuda
			"bn", // Brunei Darussalam
			"bo", // Bolivia
			"br", // Brazil
			"bs", // Bahamas
			"bt", // Bhutan
			"bv", // Bouvet Island
			"bw", // Botswana
			"by", // Belarus
			"bz", // Belize
			"ca", // Canada
			"cc", // Cocos (Keeling) Islands
			"cd", // Democratic Republic of the Congo (formerly Zaire)
			"cf", // Central African Republic
			"cg", // Republic of the Congo
			"ch", // Switzerland
			"ci", // Cè„™éº“te d'Ivoire
			"ck", // Cook Islands
			"cl", // Chile
			"cm", // Cameroon
			"cn", // China, mainland
			"co", // Colombia
			"cr", // Costa Rica
			"cu", // Cuba
			"cv", // Cape Verde
			"cx", // Christmas Island
			"cy", // Cyprus
			"cz", // Czech Republic
			"de", // Germany
			"dj", // Djibouti
			"dk", // Denmark
			"dm", // Dominica
			"do", // Dominican Republic
			"dz", // Algeria
			"ec", // Ecuador
			"ee", // Estonia
			"eg", // Egypt
			"er", // Eritrea
			"es", // Spain
			"et", // Ethiopia
			"eu", // European Union
			"fi", // Finland
			"fj", // Fiji
			"fk", // Falkland Islands
			"fm", // Federated States of Micronesia
			"fo", // Faroe Islands
			"fr", // France
			"ga", // Gabon
			"gb", // Great Britain (United Kingdom)
			"gd", // Grenada
			"ge", // Georgia
			"gf", // French Guiana
			"gg", // Guernsey
			"gh", // Ghana
			"gi", // Gibraltar
			"gl", // Greenland
			"gm", // The Gambia
			"gn", // Guinea
			"gp", // Guadeloupe
			"gq", // Equatorial Guinea
			"gr", // Greece
			"gs", // South Georgia and the South Sandwich Islands
			"gt", // Guatemala
			"gu", // Guam
			"gw", // Guinea-Bissau
			"gy", // Guyana
			"hk", // Hong Kong
			"hm", // Heard Island and McDonald Islands
			"hn", // Honduras
			"hr", // Croatia (Hrvatska)
			"ht", // Haiti
			"hu", // Hungary
			"id", // Indonesia
			"ie", // Ireland (è„™éˆ¥ç™·re)
			"il", // Israel
			"im", // Isle of Man
			"in", // India
			"io", // British Indian Ocean Territory
			"iq", // Iraq
			"ir", // Iran
			"is", // Iceland
			"it", // Italy
			"je", // Jersey
			"jm", // Jamaica
			"jo", // Jordan
			"jp", // Japan
			"ke", // Kenya
			"kg", // Kyrgyzstan
			"kh", // Cambodia (Khmer)
			"ki", // Kiribati
			"km", // Comoros
			"kn", // Saint Kitts and Nevis
			"kp", // North Korea
			"kr", // South Korea
			"kw", // Kuwait
			"ky", // Cayman Islands
			"kz", // Kazakhstan
			"la", // Laos (currently being marketed as the official domain for
					// Los Angeles)
			"lb", // Lebanon
			"lc", // Saint Lucia
			"li", // Liechtenstein
			"lk", // Sri Lanka
			"lr", // Liberia
			"ls", // Lesotho
			"lt", // Lithuania
			"lu", // Luxembourg
			"lv", // Latvia
			"ly", // Libya
			"ma", // Morocco
			"mc", // Monaco
			"md", // Moldova
			"me", // Montenegro
			"mg", // Madagascar
			"mh", // Marshall Islands
			"mk", // Republic of Macedonia
			"ml", // Mali
			"mm", // Myanmar
			"mn", // Mongolia
			"mo", // Macau
			"mp", // Northern Mariana Islands
			"mq", // Martinique
			"mr", // Mauritania
			"ms", // Montserrat
			"mt", // Malta
			"mu", // Mauritius
			"mv", // Maldives
			"mw", // Malawi
			"mx", // Mexico
			"my", // Malaysia
			"mz", // Mozambique
			"na", // Namibia
			"nc", // New Caledonia
			"ne", // Niger
			"nf", // Norfolk Island
			"ng", // Nigeria
			"ni", // Nicaragua
			"nl", // Netherlands
			"no", // Norway
			"np", // Nepal
			"nr", // Nauru
			"nu", // Niue
			"nz", // New Zealand
			"om", // Oman
			"pa", // Panama
			"pe", // Peru
			"pf", // French Polynesia With Clipperton Island
			"pg", // Papua New Guinea
			"ph", // Philippines
			"pk", // Pakistan
			"pl", // Poland
			"pm", // Saint-Pierre and Miquelon
			"pn", // Pitcairn Islands
			"pr", // Puerto Rico
			"ps", // Palestinian territories (PA-controlled West Bank and Gaza
					// Strip)
			"pt", // Portugal
			"pw", // Palau
			"py", // Paraguay
			"qa", // Qatar
			"re", // Rè„™æ¼�union
			"ro", // Romania
			"rs", // Serbia
			"ru", // Russia
			"rw", // Rwanda
			"sa", // Saudi Arabia
			"sb", // Solomon Islands
			"sc", // Seychelles
			"sd", // Sudan
			"se", // Sweden
			"sg", // Singapore
			"sh", // Saint Helena
			"si", // Slovenia
			"sj", // Svalbard and Jan Mayen Islands Not in use (Norwegian
					// dependencies; see .no)
			"sk", // Slovakia
			"sl", // Sierra Leone
			"sm", // San Marino
			"sn", // Senegal
			"so", // Somalia
			"sr", // Suriname
			"st", // Sè„™æ‹¢o Tomè„™æ¼� and Prè„™é¢…ncipe
			"su", // Soviet Union (deprecated)
			"sv", // El Salvador
			"sy", // Syria
			"sz", // Swaziland
			"tc", // Turks and Caicos Islands
			"td", // Chad
			"tf", // French Southern and Antarctic Lands
			"tg", // Togo
			"th", // Thailand
			"tj", // Tajikistan
			"tk", // Tokelau
			"tl", // East Timor (deprecated old code)
			"tm", // Turkmenistan
			"tn", // Tunisia
			"to", // Tonga
			"tp", // East Timor
			"tr", // Turkey
			"tt", // Trinidad and Tobago
			"tv", // Tuvalu
			"tw", // Taiwan, Republic of China
			"tz", // Tanzania
			"ua", // Ukraine
			"ug", // Uganda
			"uk", // United Kingdom
			"um", // United States Minor Outlying Islands
			"us", // United States of America
			"uy", // Uruguay
			"uz", // Uzbekistan
			"va", // Vatican City State
			"vc", // Saint Vincent and the Grenadines
			"ve", // Venezuela
			"vg", // British Virgin Islands
			"vi", // U.S. Virgin Islands
			"vn", // Vietnam
			"vu", // Vanuatu
			"wf", // Wallis and Futuna
			"ws", // Samoa (formerly Western Samoa)
			"ye", // Yemen
			"yt", // Mayotte
			"yu", // Serbia and Montenegro (originally Yugoslavia)
			"za", // South Africa
			"zm", // Zambia
			"zw", // Zimbabwe
	};

	private static final List INFRASTRUCTURE_TLD_LIST = Arrays
			.asList(INFRASTRUCTURE_TLDS);
	private static final List GENERIC_TLD_LIST = Arrays.asList(GENERIC_TLDS);
	private static final List COUNTRY_CODE_TLD_LIST = Arrays
			.asList(COUNTRY_CODE_TLDS);
}
