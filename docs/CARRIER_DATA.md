# Carrier Preset Data

`data/CarrierPresets.kt` lists the built-in carriers of every country and region, ordered by mobile subscribers, largest first. This file records where that order comes from and how far it can be trusted. Update it whenever the presets change, and never reorder a market from memory.

## How the data was gathered

On 2026-10-10 each of the 243 markets was researched separately. For every market the research looked for, in this order: the national regulator's market statistics, the operators' own financial reports, a government or industry body, and only then secondary sources such as trade press.

Rules applied to every market:

- Network operators only; virtual operators are left out.
- Subscribers are counted without machine-to-machine and IoT lines wherever the source separates them. This matters: in Ireland, Poland, Latvia, Italy, and South Korea the ranking changes when those lines are included.
- Names are the consumer brands subscribers see today.
- At most five operators per market.

## Status

| Status | Markets | Meaning |
| --- | --- | --- |
| Verified | 49 | Regulator or operator figures from 2024 or later cover the listed operators. |
| Partly verified | 91 | Official figures that are older, relayed by the press, or not like-for-like between operators. |
| Unverified | 103 | No usable source was found. The order is an estimate and may be wrong. |

Only verified and partly verified results were applied to the presets. Unverified markets keep their previous order; most are small island markets and markets whose regulator publishes no per-operator figures.

The links below were collected by the research run. A sample was checked for reachability, but the figures were not re-read line by line against every source.

## Territories without carriers

Antarctica, Bouvet Island, South Georgia and the South Sandwich Islands, Heard Island and McDonald Islands, the Pitcairn Islands, the French Southern Territories, and the United States Minor Outlying Islands have no mobile operator of their own and no presets. Overriding to one of them changes the region only.

## Markets

| Code | Carriers, largest first | Status | Source type | Figures as of | Source |
| --- | --- | --- | --- | --- | --- |
| `ad` | Andorra Telecom | Unverified | — | — | — |
| `ae` | e&, du | Partly verified | Operator report | FY 2025 | [e& FY 2025 results press release (e& UAE subscriber base)](https://www.zawya.com/en/press-release/companies-news/e-and-delivers-record-revenue-and-net-profit-in-fy-2025-growing-consolidated-revenue-23-to-aed-729bln-th4p4qre) |
| `af` | ATOMA, Roshan, Etisalat, AWCC, Salaam | Unverified | — | — | — |
| `ag` | Digicel, Flow, APUA inet | Unverified | — | — | — |
| `ai` | Flow, Digicel | Unverified | — | — | — |
| `al` | One, Vodafone | Verified | Regulator | Q2 2026 | [AKEP: Treguesit statistikore te tregut te komunikimeve elektronike, 3-mujori II 2026](https://akep.al/wp-content/uploads/2026/09/Treguesit-Statistikore-Komunikime-Elektronike-T2_2026-Publikim.pdf) |
| `am` | Viva, Team, Ucom | Partly verified | Secondary | 2024 | [GP Advisory: Telecommunications in Armenia (market shares attributed to Armstat)](https://gpadvisory.am/?p=4465) |
| `ao` | Unitel, Africell, Movicel | Unverified | — | — | — |
| `ar` | Claro, Personal, Movistar | Unverified | — | — | — |
| `as` | Bluesky, ASTCA | Partly verified | Operator report | FY2024-25 | [Amalgamated Telecom Holdings (ATH) Annual Report 2025](https://www.ath.com.fj/wp-content/uploads/2025/10/2025_ATH-ANNUAL-REPORT_HIGH-RES-1.pdf) |
| `at` | A1, Magenta, Drei | Verified | Regulator | Q4 2025 | [RTR Telekom Monitor Jahresbericht 2025 (Mobilfunkmarktanteile nach SIM-Karten Q4 2025, ohn](https://www.rtr.at/TKP/aktuelles/publikationen/publikationen/m/tm/RTR_Telekom_Monitor_Jahresbericht-2025_ua.pdf) |
| `au` | Telstra, Optus, Vodafone | Partly verified | Secondary | Telstra Aug 2024; Optus Jun 2024; Vodafo | [Wikipedia: List of mobile network operators of the Asia Pacific region (figures attributed](https://en.wikipedia.org/wiki/List_of_mobile_network_operators_of_the_Asia_Pacific_region) |
| `aw` | SETAR, Digicel | Unverified | — | — | — |
| `ax` | Ålcom, Elisa, Telia | Unverified | — | — | — |
| `az` | Azercell, Bakcell, Nar | Partly verified | Operator report |  | [Azercell: About us (company profile)](https://www.azercell.com/en/about-us/) |
| `ba` | BH Telecom, m:tel, HT Eronet | Partly verified | Secondary | 2024 | [U.S. International Trade Administration: Bosnia and Herzegovina Country Commercial Guide -](https://trade.gov/country-commercial-guides/bosnia-and-herzegovina-telecommunications-industry) |
| `bb` | Flow, Digicel | Unverified | — | — | — |
| `bd` | Grameenphone, Robi, Banglalink, Teletalk | Verified | Industry body | July 2026 | [AMTOB Industry Statistics: mobile phone subscribers, July 2026 (BTRC data)](https://www.amtob.org.bd/home/industrystatics) |
| `be` | Proximus, Orange, Telenet | Partly verified | Regulator | 2025 | [IBPT/BIPT: Situation du marché communications électroniques et télévision, année 2025 (pré](https://www.bipt.be/file/cc73d96153bbd5448a56f19d925d05b1379c7f21/3132ed0879773cdca04eff0e36bcf21a99ad5e72/presentation-secteur-communications-electroniques-2025.pdf) |
| `bf` | Orange, Moov Africa, Telecel | Verified | Regulator | Q2 2026 | [ARCEP Burkina Faso - Observatoire mobile, 2ème trimestre 2026](https://www.arcep.bf/download/2eme-trimestre-2026-observatoire-mobile/?wpdmdl=50864) |
| `bg` | A1, Vivacom, Yettel | Verified | Regulator | 31 December 2024 | [CRC: Annual report of the Communications Regulation Commission 2024](https://crc.bg/files/Annual_Report_CRC_2024_EN.pdf) |
| `bh` | Batelco, stc, Zain | Verified | Regulator | Q1 2026 | [TRA Bahrain Q1 2026 market indicators, as reported by Gulf Daily News - Fixed fibre drives](https://www.gdnonline.com/Details/1404253/Fixed-fibre-drives-telecom-data-consumption-growth) |
| `bi` | Lumitel, Econet Leo, Onamob | Partly verified | Regulator | Q4 2025 | [ARCT - Observatoire du marché des services de communications (voix, SMS, Internet et servi](https://arct.gov.bi/wp-content/uploads/2026/04/OBSERVATOIRE-4EME-TRIM-2026.pdf) |
| `bj` | MTN, Celtiis, Moov Africa | Verified | Regulator | Q1 2026 | [ARCEP Bénin: Observatoire de la téléphonie mobile, T1 2026](https://arcep.bj/wp-content/uploads/2026/05/Observatoire-Téléphonie-Mobile-T1_2026.pdf) |
| `bl` | Orange, Digicel, Dauphin Telecom | Unverified | — | — | — |
| `bm` | One, Digicel | Unverified | — | — | — |
| `bn` | DST, Progresif, imagine | Unverified | — | — | — |
| `bo` | Entel, Tigo, Viva | Unverified | — | — | — |
| `bq` | Digicel, Flow, Kla | Unverified | — | — | — |
| `br` | Vivo, Claro, TIM, Algar, Brisanet | Partly verified | Secondary | Q2 2026 (June 2026) | [Teletime: mercado móvel, chips, junho 2026 (dados Anatel); shares from Anatel's Q2 2026 co](https://teletime.com.br/03/08/2026/mercado-movel-chips-junho-2026/) |
| `bs` | Aliv, BTC | Partly verified | Operator report | Q4 2025 | [Liberty Latin America Form 10-K FY2025 (BTC), with URCA 2024 Annual Report total as report](https://www.sec.gov/Archives/edgar/data/1712184/000171218426000023/lila-20251231.htm) |
| `bt` | B-Mobile, TashiCell | Verified | Regulator | June 2026 | [BICMA Telecommunication Market Profile (Statistics at a Glance, as of June 30, 2026)](https://www.bicma.gov.bt/?page_id=555) |
| `bw` | Mascom, Orange, BTC | Partly verified | Operator report | Q4 2025 (Mascom subscribers); March 2022 | [MTN Group FY2025 annual financial results, subscriber data sheet (Botswana joint venture =](https://mtn-investor.com/fy-2025-reporting-suite/pdf/mtn-fy25-annual-financial-results-2025.pdf) |
| `by` | MTS, A1, life:) | Unverified | — | — | — |
| `bz` | Digi, Smart | Unverified | — | — | — |
| `ca` | Rogers, Bell, Telus, Vidéotron, SaskTel | Verified | Industry body | Q2 2026 | [Canadian Telecommunications Association: Wireless Carriers Subscriber Stats Q2 2026 (根据各运营](https://canadatelecoms.ca/wp-content/uploads/2026/09/Wireless-fr-Carriers-Sub-Stats-Q2-2026.pdf) |
| `cc` | Telstra | Unverified | — | — | — |
| `cd` | Vodacom, Airtel, Orange, Africell | Verified | Regulator | Q1 2026 | [ARPTC - Observatoire du marché de la téléphonie mobile, rapport du 1er trimestre 2026](https://arptc.gouv.cd/app/uploads/2026/06/ARPTC_RAPPORT-T1-26_15-06-26-OK.pdf) |
| `cf` | Orange, Telecel, Moov Africa | Unverified | — | — | — |
| `cg` | MTN, Airtel | Unverified | — | — | — |
| `ch` | Swisscom, Sunrise, Salt | Partly verified | Operator report | Q2 2026 (Salt: Q3 2025) | [Swisscom Interim Report January-June 2026; Sunrise investor relations page](https://www.swisscom.ch/content/dam/assets/about/investoren/berichte/documents/2026/2026-q2-interim-report.pdf) |
| `ci` | Orange, MTN, Moov Africa | Partly verified | Secondary | Q2 2025 | [Connectionivoirienne: Téléphonie mobile en Côte d'Ivoire, 2e trimestre 2025 (reporting ART](https://connectionivoirienne.net/2025/09/15/telephonie-mobile-en-cote-divoire-plus-de-257-milliards-fcfa-de-chiffre-daffaires-au-2%e1%b5%89-trimestre-2025/) |
| `ck` | Vodafone | Partly verified | Operator report | FY2024-25 | [Amalgamated Telecom Holdings (ATH) Annual Report 2025; Wikipedia list of operators](https://www.ath.com.fj/wp-content/uploads/2025/10/2025_ATH-ANNUAL-REPORT_HIGH-RES-1.pdf) |
| `cl` | Entel, Movistar, WOM, Claro | Verified | Regulator | September 2025 | [Subtel: Informe del Sector Telecomunicaciones, septiembre 2025 (Telefonía Móvil, abonados ](https://www.subtel.gob.cl/wp-content/uploads/2025/12/Informe_del_Sector_Telecomunicaciones_Sep25.pdf) |
| `cm` | Orange, MTN, Blue, Nexttel | Verified | Regulator | 2024 | [ART - Observatoire annuel 2024 du marché des communications électroniques au Cameroun (Tab](https://www.art.cm/sites/default/files/documents/Observatoire%20annuel%202024%20du%20marche%CC%81%20des%20communications%20%C3%A9lectroniques%20au%20Cameroun%20.pdf) |
| `cn` | 中国移动, 中国电信, 中国联通, 中国广电 | Verified | Operator report | Q2 2026 (中国联通: end 2025) | [China Mobile Limited - Investor Relations - Customer Data (quarterly)](https://www.chinamobileltd.com/en/ir/customer_data.php) |
| `co` | Claro, Movistar, Tigo, WOM | Verified | Regulator | March 2026 | [CRC Postdata: Data Flash Servicios Móviles 2026-1T](https://www.postdata.gov.co/sites/default/files/2026-07/Data%20Flash%20Servicios%20M%C3%B3viles%202026-1T_Publicacion.pdf) |
| `cr` | Liberty, Kölbi, Claro | Partly verified | Secondary | 2025 | [Semanario Universidad，转述 Sutel《Estadísticas del Sector de Telecomunicaciones》（2026 年 5 月发布](https://semanariouniversidad.com/pais/kolbi-ice-reporto-abrupto-aumento-de-clientes-ante-sutel-pero-ingresos-cayeron-en-mas-de-%e2%82%a120-000-millones/) |
| `cu` | Cubacel | Partly verified | Secondary | 2024 | [DPL News: Cuba / ETECSA aborda retos actuales en un encuentro con medios de prensa (quotin](https://dplnews.com/?p=265206) |
| `cv` | Alou, Unitel T+ | Partly verified | Secondary | Q1 2026 | [Forbes África Lusófona: Cabo Verde regista ligeiro aumento aos serviços de telefonia no 1.](https://forbesafricalusofona.com/?p=175051) |
| `cw` | Flow, Digicel | Unverified | — | — | — |
| `cx` | Telstra | Unverified | — | — | — |
| `cy` | Cyta, Epic, PrimeTel, Cablenet | Partly verified | Secondary | 2025-12 | [Politis（转述 OCECPR 2025 年底数据）: Cyta Strengthens Lead in Mobile Market in 2025](https://www.en.politis.com.cy/news/1006480/cyta-strengthens-lead-in-mobile-market-in-2025) |
| `cz` | T-Mobile, O2, Vodafone | Verified | Regulator | 2024 | [CTU: Zprava o vyvoji trhu elektronickych komunikaci se zamerenim na rok 2024 (Graf 20/21)](https://ctu.gov.cz/sites/default/files/obsah/stranky/472017/soubory/zovt_2024.pdf) |
| `de` | Telekom, O2, Vodafone, 1&1 | Partly verified | Regulator | Q2 2026 | [Bundesnetzagentur: Teilnehmerentwicklung im Mobilfunk (registrierte SIM-Profile nach Netzb](https://www.bundesnetzagentur.de/DE/Fachthemen/Telekommunikation/Marktdaten/Mobilfunkteilnehmer/start.html) |
| `dj` | Djibouti Telecom | Unverified | — | — | — |
| `dk` | YouSee, Norlys, Telenor, 3 | Unverified | — | — | — |
| `dm` | Digicel, Flow | Partly verified | Regulator | March 2024 | [ECTEL Annual Electronic Communications Sector Review 2023-2024（第 17 版，Table 5 与 Appendix B](https://www.ectel.int/wp-content/uploads/2026/07/Annual-Sector-Review-2023-2024.pdf) |
| `do` | Claro, Altice, Viva | Partly verified | Secondary | Q4 2025 | [elDinero: República Dominicana supera las 11 millones de líneas de comunicación móvil (Ind](https://eldinero.com.do/368757/republica-dominicana-supera-las-11-millones-de-lineas-de-comunicacion-movil/) |
| `dz` | Mobilis, Djezzy, Ooredoo | Verified | Regulator | Q3 2025 | [ARPCE - Indicateurs du marché de la téléphonie mobile (3e trimestre 2025)](https://www.arpce.dz/fr/indic/mobile) |
| `ec` | Claro, Tigo, CNT | Verified | Regulator | July 2026 | [ARCOTEL: Reporte Estadístico Mensual, julio 2026 (Servicio Móvil Avanzado, líneas activas)](https://www.arcotel.gob.ec/wp-content/uploads/2026/09/7.-Julio-2026.pdf) |
| `ee` | Telia, Elisa, Tele2 | Partly verified | Secondary | 2024 | [Telecompaper: Telia maintains lead in Estonian mobile market (citing regulator TTJA)](https://www.telecompaper.com/news/telia-maintains-lead-in-estonian-mobile-market--1534157) |
| `eg` | Vodafone, Orange, e&, WE | Partly verified | Operator report | Q2 2026 | [Vodacom Group quarterly trading update for the quarter ended 30 June 2026 (Egypt segment)](https://www.vodacom.com/pdf/investor/quarterly-results/2026/vodacom-trading-update-1q27.pdf) |
| `eh` | Orange, inwi, Maroc Telecom | Unverified | — | — | — |
| `er` | EriTel | Unverified | — | — | — |
| `es` | Orange, Movistar, Vodafone, DIGI | Partly verified | Secondary | 2025-12（Movistar 为 2026-05） | [Wikipedia: List of mobile network operators in Europe (Spain, 引用运营商与 CNMC 数据)；CNMC 月度电信数据新](https://en.wikipedia.org/wiki/List_of_mobile_network_operators_in_Europe) |
| `et` | Ethio Telecom, Safaricom | Unverified | — | — | — |
| `fi` | Elisa, DNA, Telia | Unverified | — | — | — |
| `fj` | Vodafone, Digicel | Partly verified | Operator report | FY2024-25 (year to 31 Mar 2025) | [Amalgamated Telecom Holdings (ATH) Annual Report 2025](https://www.ath.com.fj/wp-content/uploads/2025/10/2025_ATH-ANNUAL-REPORT_HIGH-RES-1.pdf) |
| `fk` | Sure | Unverified | — | — | — |
| `fm` | FSM Telecom | Unverified | — | — | — |
| `fo` | Føroya Tele, Nema | Unverified | — | — | — |
| `fr` | Orange, SFR, Bouygues Telecom, Free | Unverified | — | — | — |
| `ga` | Moov Africa, Airtel | Unverified | — | — | — |
| `gb` | O2, EE, Vodafone, Three | Partly verified | Operator report | Q2 2026 (EE: Q3 2025) | [Virgin Media O2 Q2 2026 Earnings Release; Vodafone Group Q1 FY27 trading update (additiona](https://news.virginmediao2.co.uk/wp-content/uploads/2026/07/Virgin-Media-O2-Q2-2026-Earnings-Release.pdf) |
| `gd` | Digicel, Flow | Partly verified | Regulator | March 2024 | [ECTEL Annual Electronic Communications Sector Review 2023-2024（第 17 版，Table 6 与 Appendix B](https://www.ectel.int/wp-content/uploads/2026/07/Annual-Sector-Review-2023-2024.pdf) |
| `ge` | Magti, Silknet, Cellfie | Verified | Regulator | 2024 | [ComCom (Georgian National Communications Commission): 2024 Activity Report summary - numbe](https://comcom.ge/en/yvela-siaxle/number-of-mobile-internet-subscribers-reaches-46-million-in-2024.page) |
| `gf` | Orange, SFR, Digicel | Unverified | — | — | — |
| `gg` | Sure, JT | Verified | Regulator | 2025 | [JCRA Telecoms Market Report 2025 (joint Channel Islands report covering Guernsey and Jerse](https://www.jcra.je/media/yerl5wva/telecoms-market-report-2025.pdf) |
| `gh` | MTN, Telecel, AT | Verified | Regulator | June 2026 | [NCA Industry Market Statistics: MNOs Voice Subscription Analysis, January to June 2026](https://nca.org.gh/wp-content/uploads/2026/07/Monthly-Industry-Voice_June_2026.pdf) |
| `gi` | Gibtelecom | Partly verified | Regulator | FY 2025/26 (to March 2026) | [Gibraltar Regulatory Authority Annual Report 2025/2026](https://www.gra.gi/uploads/documents/Home/Annual%20Reports/GRA%20Annual%20Report%202026.pdf) |
| `gl` | Tusass | Unverified | — | — | — |
| `gm` | Africell, QCell, Comium, Gamcel | Partly verified | Regulator | December 2020 | [PURA: Overview of Mobile Network Operators](https://pura.gm/ict/sub-sectors/mobile-network-operators) |
| `gn` | Orange, MTN, Cellcom | Partly verified | Secondary | Q2 2025 | [Osiris / Agence Ecofin: Guinée, la crise sociale et financière à Cellcom (citing ARPT repo](https://osiris.sn/guinee-la-crise-sociale-et-financiere-a-cellcom-presage-un-renforcement-de-la.html) |
| `gp` | Orange, Digicel, SFR, Free | Partly verified | Regulator | 2025 | [Arcep – Le marché des services de communications électroniques outre-mer, année 2025（2026 ](https://www.arcep.fr/fileadmin/cru-1784810792/reprise/observatoire/march-an2025/obs-marches-2025-OUTRE-MER_juil2026.pdf) |
| `gq` | GETESA, Muni | Unverified | — | — | — |
| `gr` | Cosmote, Vodafone, Nova | Partly verified | Secondary | 2024 | [Opensignal Greece Mobile Network Experience Report, September 2026（引用 EETT 2026 年 2 月 Mark](https://insights.opensignal.com/reports/2026/09/greece/mobile-network-experience) |
| `gt` | Tigo, Claro | Partly verified | Secondary | 2025 | [TeleSemana: Panorama de Mercado – Guatemala (2025 年 6 月更新)](https://www.telesemana.com/panorama-de-mercado/guatemala/) |
| `gu` | GTA, Docomo Pacific, IT&E | Unverified | — | — | — |
| `gw` | Orange, Telecel | Unverified | — | — | — |
| `gy` | Digicel, One, ENet | Unverified | — | — | — |
| `hk` | CMHK, csl, 3, SmarTone | Partly verified | Secondary | 2023-2026 (mixed: csl and SmarTone Dec 2 | [Wikipedia: List of mobile network operators of the Asia Pacific region (Hong Kong section)](https://en.wikipedia.org/wiki/List_of_mobile_network_operators_of_the_Asia_Pacific_region) |
| `hn` | Tigo, Claro, Hondutel | Partly verified | Secondary | Q1 2025 | [DPL News Digital Metrics，转述 CONATEL 季度行业报告](https://dplnews.com/suscriptores-telefonia-movil-siguen-a-la-baja-honduras/) |
| `hr` | Hrvatski Telekom, A1, Telemach | Partly verified | Regulator | 2025 | [HAKOM: Godisnje izvjesce o radu za 2025.](https://www.sabor.hr/sites/default/files/uploads/sabor/2026-05-29/161102/IZVJ_HAKOM_2025.pdf) |
| `ht` | Digicel, Natcom | Unverified | — | — | — |
| `hu` | Telekom, One, Yettel | Verified | Regulator | Q4 2025 | [NMHH: Mobilpiaci jelentes - 2025. II. felev](https://nmhh.hu/cikk/259107/Mobilpiaci_jelentes__2025_II_felev) |
| `id` | Telkomsel, Indosat, XL, Smartfren | Partly verified | Secondary | Q3 2025 | [Selular.ID (Nov 2025) relaying operator quarterly reports; Bisnis.com (Apr 2026) as summar](https://selular.id/2025/11/pasar-sudah-berubah-operator-kini-tak-lagi-mengejar-sebanyak-banyaknya-jumlah-pelanggan/) |
| `ie` | Vodafone, Three, eir | Verified | Regulator | Q2 2026 | [ComReg Quarterly Key Data Report Q2 2026 (ComReg 26/59)](https://www.comreg.ie/publication-download/quarterly-key-data-report-q2-2026) |
| `il` | Cellcom, Partner, Pelephone, HOT Mobile, wecom | Partly verified | Secondary | Q2 2026 | [Wikipedia - List of mobile network operators of the Middle East and Africa (Israel section](https://en.wikipedia.org/wiki/List_of_mobile_network_operators_of_the_Middle_East_and_Africa) |
| `im` | Manx Telecom, Sure | Partly verified | Regulator | 2026 | [CURA (Communications and Utilities Regulatory Authority): Mobile Service Providers](https://www.cura.im/mobile/mobile-service-providers/) |
| `in` | Jio, Airtel, Vi, BSNL | Verified | Regulator | August 2026 | [TRAI Press Release No. 125/2026: Telecom Subscription Data as on 31 August 2026 (Annexure-](https://www.trai.gov.in/sites/default/files/2026-09/PR_No125of2026_0.pdf) |
| `io` | Sure | Partly verified | Operator report | 2026 | [Sure Diego Garcia website](https://www.sure.io/) |
| `iq` | Zain, Asiacell, Korek | Partly verified | Operator report | FY 2025 | [Zain Group Sustainability Report 2025 - Customer Base; Ooredoo Group FY 2025 results (Asia](https://zain.com/SR2025/wp-content/uploads/2026/04/16-Customer-Base.pdf) |
| `ir` | MCI, Irancell, RighTel | Unverified | — | — | — |
| `is` | Síminn, Nova, Vodafone | Unverified | — | — | — |
| `it` | Vodafone, WindTre, TIM, Iliad | Partly verified | Secondary | 2025-09 | [Switcho（转述 AGCOM Osservatorio sulle comunicazioni，数据截至 2025 年 9 月）](https://www.switcho.it/blog/mobile/osservatorio-agcom-gennaio-2026/) |
| `je` | JT, Sure | Verified | Regulator | 2025 | [JCRA Telecoms Market Report 2025 (4insight, July 2026)](https://www.jcra.je/media/yerl5wva/telecoms-market-report-2025.pdf) |
| `jm` | Digicel, Flow | Partly verified | Regulator | Q4 2025 | [OUR Telecommunications Market Information Report: October - December 2025 (total), with Li](https://our.org.jm/wp-content/uploads/2026/08/Telecommunications-Market-Information-Report-October-December-2025.pdf) |
| `jo` | Zain, Orange, Umniah | Partly verified | Operator report | FY 2025 | [Zain Group Sustainability Report 2025 - Customer Base (active customers at 31 December 202](https://zain.com/SR2025/wp-content/uploads/2026/04/16-Customer-Base.pdf) |
| `jp` | NTT docomo, au, SoftBank, Rakuten Mobile | Verified | Regulator | end March 2026 (FY2025 Q4) | [総務省: 電気通信サービスの契約数及びシェアに関する四半期データの公表（令和7年度第4四半期（3月末））](https://www.soumu.go.jp/main_content/001081665.pdf) |
| `ke` | Safaricom, Airtel, Faiba, Telkom | Verified | Regulator | Q2 2026 | [Communications Authority of Kenya - Sector Statistics Report Q4 2025-2026 (Figure 7: Mobil](https://ca.go.ke/sites/default/files/2026-09/Sector%20Statistics%20Report%20Q4%202025-2026_0.pdf) |
| `kg` | O!, MEGA, Beeline | Unverified | — | — | — |
| `kh` | Metfone, Smart, Cellcard | Unverified | — | — | — |
| `ki` | Vodafone, Ocean Link | Partly verified | Operator report | FY2024-25 | [Amalgamated Telecom Holdings (ATH) Annual Report 2025](https://www.ath.com.fj/wp-content/uploads/2025/10/2025_ATH-ANNUAL-REPORT_HIGH-RES-1.pdf) |
| `km` | Huri, Yas | Unverified | — | — | — |
| `kn` | Flow, Digicel | Partly verified | Regulator | March 2024 | [ECTEL Annual Electronic Communications Sector Review 2023-2024（第 17 版，Table 7 与 Appendix B](https://www.ectel.int/wp-content/uploads/2026/07/Annual-Sector-Review-2023-2024.pdf) |
| `kp` | Koryolink, Kangsong NET | Unverified | — | — | — |
| `kr` | SK Telecom, KT, LG U+ | Verified | Regulator | 2026-07 | [과학기술정보통신부: (2026년 7월 말 기준) 유·무선통신서비스 가입 현황 및 무선데이터 트래픽 통계](https://www.msit.go.kr/bbs/view.do?sCode=user&mId=99&mPid=74&bbsSeqNo=79&nttSeqNo=3173725) |
| `kw` | Ooredoo, Zain, stc | Partly verified | Operator report | FY 2025 | [Ooredoo Kuwait Group FY 2025 results press release; Zain Group Sustainability Report 2025;](https://www.zawya.com/en/press-release/companies-news/ooredoo-kuwait-group-reported-strong-9-growth-in-revenue-to-reach-kwd-774mln-in-2025-wl7ozscw) |
| `ky` | Flow, Digicel | Partly verified | Regulator | Q2 2026 | [OfReg / ICTA – Cayman Islands ICT Statistics（2026-08-07 更新）](https://www.ofreg.ky/viewPDF/documents/2026-08-20-12-58-01-CaymanIslandsICTstatistics.pdf) |
| `kz` | Beeline, Tele2, Altel, Kcell, activ | Partly verified | Operator report | 2025 | [VEON 3Q2025 Factbook (Kazakhstan mobile customers); Kcell JSC FY2025 results release filed](https://www.veon.com/fileadmin/user_upload/investors/reports/2025/3Q2025_Factbook.pdf) |
| `la` | Unitel, Lao Telecom, ETL, Best Telecom | Unverified | — | — | — |
| `lb` | touch, Alfa | Unverified | — | — | — |
| `lc` | Digicel, Flow | Partly verified | Regulator | March 2024 | [ECTEL Annual Electronic Communications Sector Review 2023-2024（第 17 版，Table 8 与 Appendix B](https://www.ectel.int/wp-content/uploads/2026/07/Annual-Sector-Review-2023-2024.pdf) |
| `li` | FL1, 7acht, Swisscom | Unverified | — | — | — |
| `lk` | Dialog, SLT-Mobitel, Hutch | Partly verified | Operator report | Q1 2026 (Dialog); Q4 2024 (SLT-Mobitel); | [Dialog Axiata PLC Earnings Call Q1 2026 (investor forum presentation); SLT-Mobitel Annual ](https://s3.ap-southeast-1.amazonaws.com/dlg.dialog.lk/s3fs-public/2026-05/q1-investor-forum-2026-v2.pdf) |
| `lr` | Orange, Lonestar Cell MTN | Verified | Regulator | Mid-2025 | [Liberia Telecommunications Authority: 2025 Mid-Year Data Report](https://lta.gov.lr/wp-content/uploads/2026/04/2025-midyear-DATA-REPORT.pdf) |
| `ls` | Vodacom, Econet | Partly verified | Operator report | Q2 2026 (Vodacom only) | [Vodacom Group trading update for the quarter ended 30 June 2026 (International business cu](https://www.vodacom.com/pdf/investor/quarterly-results/2026/vodacom-trading-update-1q27.pdf) |
| `lt` | Tele2, Telia, Bitė | Unverified | — | — | — |
| `lu` | POST, Tango, Orange | Verified | Regulator | 2025 | [ILR: Télécommunications du Luxembourg - Rapport statistique 2025 (12/06/2026)](https://www.ilr.lu/wp-content/uploads/statistiques/ilr-stat-12062026-Rapport-telecom-2025.pdf) |
| `lv` | Tele2, LMT, Bite | Partly verified | Secondary | February 2024 | [Telecompaper: Tele2 claims lead on Latvian mobile subscriber market (citing regulator SPRK](https://www.telecompaper.com/news/tele2-claims-lead-on-latvian-mobile-subscriber-market--1493836) |
| `ly` | Libyana, Almadar | Unverified | — | — | — |
| `ma` | Orange, inwi, Maroc Telecom | Verified | Regulator | Q1 2026 | [ANRT - Observatoire de la téléphonie mobile, T1 2026](https://www.anrt.ma/sites/default/files/2026-06/Observatoire%20ANRT%20-%20Mobile%20-%20T1-2026.pdf) |
| `mc` | Monaco Telecom | Unverified | — | — | — |
| `md` | Orange, Moldcell, Moldtelecom | Verified | Regulator | Q2 2026 | [ANRCETI: Raport statistic comunicatii electronice, trimestrul II 2026](https://ro.anrceti.md/files/filefield/Raport%20CE%20tr.II%202026.pdf) |
| `me` | m:tel, Crnogorski Telekom, One | Verified | Regulator | June 2025 | [EKIP: Informacija o stanju trzista elektronskih komunikacija za jun 2025. godine - Mobilna](https://www.ekip.me/media/documents/general/1753445659_Mobilna - jun.pdf) |
| `mf` | Orange, Digicel, Dauphin Telecom | Unverified | — | — | — |
| `mg` | Yas, Orange, Airtel | Unverified | — | — | — |
| `mh` | MINTA | Unverified | — | — | — |
| `mk` | A1, Telekom, m:tel | Verified | Regulator | Q1 2026 | [AEK: Informacii za sostojbata na pazarot za elektronski komunikacii vo mobilna i fiksna te](https://aek.mk/wp-content/uploads/2026/07/20260707-Информации-за-состојбата-на-пазарот-фиксна-првиот-квартал-2026.pdf) |
| `ml` | Orange, Moov Africa, Telecel | Partly verified | Secondary | 2023 | [Agence Ecofin via OSIRIS (22 Nov 2024), citing AMRTP statistics; Maroc Telecom H1 2025 res](https://www.osiris.sn/mali-orange-a-renforce-son-leadership-dans-le-mobile-money-entre-2020-et-2023.html) |
| `mm` | MPT, Atom, Ooredoo, Mytel | Unverified | — | — | — |
| `mn` | Mobicom, Unitel, Skytel, G-Mobile, ONDO | Partly verified | Regulator | 2022 | [Communications Regulatory Commission of Mongolia: statistics presentation 2022 (2.2 Mobile](https://www.crc.gov.mn/storage/%D0%97%D0%97%D2%AE%D0%A2%D0%97%D0%93/statistic/PPT%202022%20Year%20english%20version.pdf) |
| `mo` | CTM, China Telecom | Partly verified | Operator report | 30 June 2026 | [CITIC Telecom International Holdings - 2026 Interim Report (CTM mobile subscribers and mar](https://www.citictel.com/wp-content/uploads/2026/09/e1883.pdf) |
| `mp` | IT&E, Docomo Pacific | Unverified | — | — | — |
| `mq` | Orange, Digicel, SFR, Free | Partly verified | Regulator | 2025 | [Arcep – Le marché des services de communications électroniques outre-mer, année 2025（2026 ](https://www.arcep.fr/fileadmin/cru-1784810792/reprise/observatoire/march-an2025/obs-marches-2025-OUTRE-MER_juil2026.pdf) |
| `mr` | Moov Mauritel, Chinguitel, Mattel | Partly verified | Secondary | 2025 | [BTW Media - Mauritel profile, quoting Maroc Telecom's Mauritel subsidiary page (iam.ma)](https://btw.media/en/mauritel-and-the-desert-coverage-bill-behind-a-cheap-prepaid-megabyte) |
| `ms` | Flow, Digicel | Unverified | — | — | — |
| `mt` | Epic, GO, Melita | Partly verified | Secondary | 2025-12 | [TS2 / Stockti（转述 Malta Communications Authority 2025 全年市场数据）: Melita's Epic acquisition wo](https://ts2.tech/en/melitas-epic-acquisition-would-combine-64-of-maltas-mobile-subscriptions/) |
| `mu` | my.t, Emtel, Chili | Unverified | — | — | — |
| `mv` | Dhiraagu, Ooredoo | Unverified | — | — | — |
| `mw` | Airtel, TNM | Unverified | — | — | — |
| `mx` | Telcel, AT&T, Movistar | Partly verified | Secondary | Q2 2025 | [Xataka México，转述 CRT（Comisión Reguladora de Telecomunicaciones）首份行业统计报告，2025 年第二季度](https://www.xataka.com.mx/telecomunicaciones/mexico-pone-orden-cifras-lineas-moviles-bait-cuarto-lugar-8-3-millones-telcel-lider) |
| `my` | CelcomDigi, Maxis, U Mobile, YES, unifi | Partly verified | Secondary | CelcomDigi and Maxis: Q2 2026; U Mobile  | [Wikipedia: List of mobile network operators of the Asia Pacific region (Malaysia table, ci](https://en.wikipedia.org/wiki/List_of_mobile_network_operators_of_the_Asia_Pacific_region) |
| `mz` | Vodacom, Movitel, Tmcel | Unverified | — | — | — |
| `na` | MTC, TN Mobile | Unverified | — | — | — |
| `nc` | Mobilis | Unverified | — | — | — |
| `ne` | Airtel, Moov Africa, Zamani, Niger Telecoms | Verified | Regulator | Q3 2025 | [ARCEP Niger - Rapport d'observatoire des communications électroniques, 3ème trimestre 2025](https://arcep.ne/wp-content/uploads/2026/01/Rapport-dObservatoire-des-CE_T3-FIN_09_25.pdf) |
| `nf` | Norfolk Telecom | Unverified | — | — | — |
| `ng` | MTN, Airtel, Glo, T2 | Partly verified | Secondary | July 2026 | [Arise News: MTN crosses 100 million subscribers as Nigeria's telecoms base hits 195 millio](https://www.arise.tv/mtn-crosses-100-million-subscribers-as-nigerias-telecoms-base-hits-195-million/) |
| `ni` | Claro, Tigo | Unverified | — | — | — |
| `nl` | Odido, KPN, Vodafone | Partly verified | Regulator | Q1 2026 | [ACM Telecommonitor Q1 2026 (Mobiele markt ziet lichte daling); ACM Telecommonitor Q4 2025](https://www.acm.nl/nl/publicaties/mobiele-markt-ziet-lichte-daling) |
| `no` | Telenor, Telia, Ice | Verified | Regulator | 2025-12 | [Nkom: Ekomstatistikken helår 2025, Figur 9 Markedsandeler for mobiltelefoniabonnement](https://nkom.no/statistikk/statiske-rapporter-og-analyser/_/attachment/inline/043adde1-4792-47a1-821d-66c645086e35:b7fc0f6620b59e582343a7589cd4205753a4aa2b/Ekomstatistikken%202025%20(2).pdf) |
| `np` | Nepal Telecom, Ncell | Partly verified | Secondary | Jestha 2083 (mid-May to mid-June 2026) | [Himal Press, 'Total telephone subscriptions in Nepal hit 30.31 million' (9 Jul 2026), repo](https://en.himalpress.com/?p=85701) |
| `nr` | Digicel | Unverified | — | — | — |
| `nu` | Telecom Niue | Unverified | — | — | — |
| `nz` | Spark, One NZ, 2degrees | Verified | Regulator | Jun 2025 | [Commerce Commission, 2025 Telecommunications Monitoring Report (Figure 5.14 MNO market sha](https://www.comcom.govt.nz/assets/Uploads/2025-Telecommunications-Monitoring-Report-29-June-2026.pdf) |
| `om` | Omantel, Ooredoo, Vodafone | Partly verified | Operator report | FY 2025 | [Ooredoo Oman IR Call Q4 2025 presentation (total mobile customers)](https://www.ooredoo.om/wp-content/uploads/calendar/presentation/Ooredoo_Oman_IR_Call_Q4-2025.pdf) |
| `pa` | Tigo, +Móvil | Partly verified | Secondary | 2025 | [DPL News: ASEP buscará consolidar un tercer operador móvil en Panamá este 2026 (2026-03-23](https://dplnews.com/asep-buscara-consolidar-tercer-operador-movil-panama/) |
| `pe` | Claro, Bitel, Entel, Movistar | Verified | Regulator | Q1 2026 | [OSIPTEL Nota de Prensa N.º 78-2026: Mercado peruano alcanzó los 39.4 millones de líneas mó](https://www.osiptel.gob.pe/media/ekvp4ujq/np-8-6-2026-mercado-peruano-servicio-móvil.pdf) |
| `pf` | Vini, Vodafone, Viti | Unverified | — | — | — |
| `pg` | Digicel, Vodafone, Telikom | Verified | Regulator | 2025 | [NICTA Annual Performance and Management Report 2025 (Retail Mobile Subscribers and Penetra](https://www.nicta.gov.pg/wp-content/uploads/2025-NICTA-Annual-Report-Print.pdf) |
| `ph` | Globe, Smart, DITO | Partly verified | Operator report | Globe: Jun 2026; DITO: Mar 2026; Smart:  | [Globe 2Q26 results press release; DITO figure from Philstar (15 Jun 2026); Smart figure fr](https://www.globe.com.ph/sites/default/files/reports/secpse/2026/B.%20Quarterly%20Reports/IV.%20Quarterly%20Results%20Press%20Release/GLO-2Q26-Press-Release.pdf) |
| `pk` | Jazz, Zong, Telenor, Ufone, SCOM | Partly verified | Secondary | August 2026 | [PTA Telecom Indicators (August 2026), as transcribed in Wikipedia 'List of mobile network ](https://www.pta.gov.pk/category/telecom-indicators) |
| `pl` | Play, Orange, T-Mobile, Plus | Partly verified | Secondary | 2024 | [Rzeczpospolita: UKE: Rynek telekomunikacyjny wyhamowal (reporting UKE's Raport o stanie ry](https://www.rp.pl/biznes/art42624821-uke-rynek-telekomunikacyjny-wyhamowal-rachunki-w-gore) |
| `pm` | SPM Telecom, Globaltel | Unverified | — | — | — |
| `pr` | Claro, Liberty, T-Mobile | Unverified | — | — | — |
| `ps` | Jawwal, Ooredoo | Partly verified | Operator report | FY 2025 | [Ooredoo Kuwait Group FY 2025 results press release (Ooredoo Palestine customers)](https://www.zawya.com/en/press-release/companies-news/ooredoo-kuwait-group-reported-strong-9-growth-in-revenue-to-reach-kwd-774mln-in-2025-wl7ozscw) |
| `pt` | MEO, NOS, Vodafone, DIGI | Partly verified | Secondary | Q1 2026 | [4gnews（转述 ANACOM 2026 年第一季度移动服务统计）: Consumo de internet móvel dispara em Portugal e DIGI c](https://4gnews.pt/consumo-de-internet-movel-dispara-em-portugal-e-digi-continua-a-ganhar-clientes/) |
| `pw` | PNCC, PT Waves | Unverified | — | — | — |
| `py` | Tigo, Personal, Claro | Unverified | — | — | — |
| `qa` | Ooredoo, Vodafone | Verified | Operator report | FY 2025 | [Ooredoo Group FY 2025 results (Qatar customers), as reported by Qatar Tribune](https://www.qatar-tribune.com/article/218708/business/ooredoo-group-net-profit-rises-12-to-qr39-billion-in-2025) |
| `re` | Orange, SFR, Free, Zeop | Unverified | — | — | — |
| `ro` | Digi, Orange, Vodafone | Partly verified | Secondary | H2 2025 | [Economedia / HotNews reporting ANCOM market data for end-2025 (published June 2026)](https://economedia.ro/ancom-digi-devine-lider-pe-piata-de-telefonie-mobila-din-romania-cu-peste-77-milioane-de-utilizatori-si-o-cota-de-aproape-34.html) |
| `rs` | mts, Yettel, A1 | Verified | Regulator | Q4 2025 | [RATEL: Overview of the electronic communications market, Q4 2025](https://www.ratel.rs/storage/upload/2026/03/Q4-2025.pdf) |
| `ru` | MTS, MegaFon, t2, Beeline | Unverified | — | — | — |
| `rw` | MTN, Airtel | Verified | Regulator | Q1 2026 | [RURA - ICT Sector Statistics Report as of the First Quarter 2026 (Table 2)](https://rura.rw/fileadmin/user_upload/RURA/Documents/Sectors/ICT/Statistics/Quarterly_publication/ICT_Sector_Statistics_Report_as_of_the_First_Quarter_2026.pdf) |
| `sa` | stc, Mobily, Zain | Partly verified | Secondary | FY 2025 (subscribers); 2026 (shares) | [Argaam - Saudi telecom networks transform; quality alone doesn't guarantee market share: O](https://www.argaam.com/en/article/articledetail/id/1929694) |
| `sb` | Our Telekom, bmobile | Unverified | — | — | — |
| `sc` | Cable & Wireless, Airtel, Intelvision | Unverified | — | — | — |
| `sd` | Zain, MTN, Sudani | Unverified | — | — | — |
| `se` | Telia, Tele2, Telenor, Tre | Verified | Regulator | 2025-12 | [PTS: Svensk telekommarknad 2025 (PTS-ER-2026:13), Tabell 12 Marknadsandelar för mobilabonn](https://www.pts.se/contentassets/9f77870e1a904af1978495986b9a227c/slutversion-rapport-svensk-telekommarknad-2025-dnr-2516551_t.pdf) |
| `sg` | Singtel, StarHub, M1, Simba | Partly verified | Secondary | Singtel and StarHub: 31 Mar 2026; SIMBA: | [Operator Watch blog (August 2026) compiling operator-reported figures; Tuas FY26 results c](https://www.operatorwatch.com/2026/08/?m=0) |
| `sh` | Sure | Partly verified | Operator report | 2026 | [Sure South Atlantic (St Helena) website](https://www.sure.co.sh/) |
| `si` | Telekom Slovenije, Telemach, A1, T-2 | Partly verified | Secondary | Q2 2025 | [Telecompaper: Telekom Slovenije increases lead in mobile market in Q2 (citing regulator AK](https://www.telecompaper.com/news/telekom-slovenije-increases-lead-in-mobile-market-in-q2--1549237) |
| `sj` | Telenor, Telia | Unverified | — | — | — |
| `sk` | O2, Orange, Telekom, 4ka | Partly verified | Secondary | Q3 2025 (4ka: Q2 2025) | [TouchIT: Zmena medzi operatormi: Toto je nova jednotka na Slovensku (compiled from O2, Ora](https://touchit.sk/zmena-medzi-operatormi-toto-je-nova-jednotka-na-slovensku/818981/) |
| `sl` | Africell, Orange, QCell | Unverified | — | — | — |
| `sm` | TIM San Marino, San Marino Telecom | Unverified | — | — | — |
| `sn` | Orange, Yas, Expresso | Partly verified | Secondary | Q1 2025 | [Afrique IT News: Yas et Promobile redessinent le paysage télécom au Sénégal, Orange encore](https://afriqueitnews.com/tech-media/yas-promobile-redessinent-paysage-telecom-senegal-orange-encore-dominant/) |
| `so` | Hormuud, Somtel, Telesom, Golis | Unverified | — | — | — |
| `sr` | Telesur, Digicel | Unverified | — | — | — |
| `ss` | MTN, Zain, Digitel | Unverified | — | — | — |
| `st` | CST, Unitel | Unverified | — | — | — |
| `sv` | Tigo, Claro, Movistar, Digicel | Unverified | — | — | — |
| `sx` | TelCell, Flow | Unverified | — | — | — |
| `sy` | Syriatel, MTN | Unverified | — | — | — |
| `sz` | MTN, Eswatini Mobile | Partly verified | Operator report | Q4 2025 (MTN only) | [MTN Group FY2025 annual financial results, subscriber data sheet (Eswatini joint venture)](https://mtn-investor.com/fy-2025-reporting-suite/pdf/mtn-fy25-annual-financial-results-2025.pdf) |
| `tc` | Flow, Digicel | Partly verified | Regulator | FY2024/25 | [TCI Telecommunications Commission – Annual Report 2024-25](https://telecommission.tc/wp-content/uploads/2026/10/TCI-Telecommunication-Commission-Annual-Report-2024-25-Compressed.pdf) |
| `td` | Airtel, Moov Africa | Partly verified | Secondary | 2024 | [EcoMatin (3 Oct 2025) reporting ARCEP Tchad's 2024 Observatoire du marché](https://ecomatin.net/tchad-le-nombre-dabonnes-a-la-telephonie-mobile-frole-15-millions-arcep) |
| `tg` | Yas, Moov Africa | Verified | Regulator | Q1 2026 | [ARCEP Togo: Observatoire des marchés des communications électroniques, T1 2026](https://arcep.tg/wp-content/uploads/2026/06/Obs-marches_T1-2026.pdf) |
| `th` | True, AIS, NT | Verified | Operator report | Q2 2026 | [AIS Investor Presentation, Singtel Investor Day 2026 (mobile subscriber market share as of](https://links.sgx.com/1.0.0/corporate-announcements/423V7E007WOXU5ET/902535_Investor%20Day%202026%20-%20AIS.pdf) |
| `tj` | Tcell, MegaFon, Babilon-M, ZET-Mobile | Unverified | — | — | — |
| `tk` | Teletok | Unverified | — | — | — |
| `tl` | Telemor, Timor Telecom, Telkomcel | Unverified | — | — | — |
| `tm` | TM CELL | Unverified | — | — | — |
| `tn` | Ooredoo, Tunisie Telecom, Orange | Verified | Regulator | Q2 2025 | [Instance Nationale des Télécommunications - Tableau de bord trimestriel TR2-2025](https://www.intt.tn/upload/files/TB-Trimestriel-TR2-2025.pdf) |
| `to` | Digicel, U-Call | Unverified | — | — | — |
| `tr` | Turkcell, Türk Telekom, Vodafone | Verified | Regulator | Q1 2026 | [BTK - Türkiye Elektronik Haberleşme Sektörü Üç Aylık Pazar Verileri Raporu, 2026 1. Çeyrek](https://www.btk.gov.tr/s3/web-btk-site/7708c26c-5161-4919-8b3d-daa31d4ac8fb/2026/06/71082aad-cfbc-47b0-9b03-8295991140a6.pdf) |
| `tt` | Digicel, bmobile | Unverified | — | — | — |
| `tv` | Tuvalu Telecom | Unverified | — | — | — |
| `tw` | 中華電信, 台灣大哥大, 遠傳電信 | Partly verified | Secondary | 2026-08 | [手機王 SOGI: NCC 115 年 8 月行動寬頻用戶數統計報導](https://www.sogi.com.tw/articles/ncc-5g/6269853) |
| `tz` | Vodacom, Yas, Airtel, Halotel, TTCL | Verified | Regulator | Q2 2026 | [TCRA - Communications Sector Performance Report, quarter ending June 2026 (Table 1.1.2.1 a](https://www.tcra.go.tz/tcra-tovuti/2026/mamlaka-website/documents/quarterly-statistics/Communications_Sector_performance_report_for_quarter_ending_June_2026_ad44159230.pdf) |
| `ua` | Kyivstar, Vodafone, lifecell | Unverified | — | — | — |
| `ug` | MTN, Airtel, Lycamobile | Partly verified | Operator report | Q2 2026 | [MTN Uganda H1 2026 Results Presentation; UCC Market Performance Report Q2 2026](https://www.mtn.co.ug/wp-content/uploads/sites/7/2026/08/MTN-Uganda-Limited-H1-2026-Results-Presentation.pdf) |
| `us` | Verizon, T-Mobile, AT&T | Partly verified | Secondary | Q2 2026 | [TelecomLead: Telecom Subscriber Growth 2026 (转述三家运营商 Q2 2026 财报数据)](https://telecomlead.com/telecom-services/telecom-subscriber-growth-2026-which-operators-added-the-most-customers-127908) |
| `uy` | Antel, Tigo, Claro | Partly verified | Government | December 2025 (Antel); June 2025 (Tigo,  | [Presidencia de Uruguay: Antel líder del mercado, Informe de Mercado de Telecomunicaciones ](https://www.gub.uy/presidencia/comunicacion/noticias/antel-lider-mercado-informe-ursec-2025) |
| `uz` | Ucell, Mobiuz, Uzmobile, Beeline | Partly verified | Secondary | Q4 2024 - Q3 2025 | [Wikipedia: List of mobile network operators of the Asia Pacific region (Uzbekistan table, ](https://en.wikipedia.org/wiki/List_of_mobile_network_operators_of_the_Asia_Pacific_region) |
| `va` | Vodafone, TIM | Unverified | — | — | — |
| `vc` | Digicel, Flow | Partly verified | Regulator | March 2024 | [ECTEL Annual Electronic Communications Sector Review 2023-2024（第 17 版，Table 9 与 Appendix B](https://www.ectel.int/wp-content/uploads/2026/07/Annual-Sector-Review-2023-2024.pdf) |
| `ve` | Movistar, Digitel, Movilnet | Verified | Regulator | Q4 2025 | [CONATEL: Telefonía Móvil, estimaciones al cierre del IV Trimestre de 2025 (cifras prelimin](https://conatel.gob.ve/wp-content/uploads/2026/07/PDF_TM_IV_2025.pdf) |
| `vg` | CCT, Flow, Digicel | Unverified | — | — | — |
| `vi` | Liberty, T-Mobile, Viya | Unverified | — | — | — |
| `vn` | Viettel, MobiFone, Vinaphone, Vietnamobile | Partly verified | Secondary | 2023 | [Wikipedia: MobiFone (market shares for 2023 cited to the Ministry of Information and Commu](https://en.wikipedia.org/wiki/MobiFone) |
| `vu` | Digicel, Vodafone | Unverified | — | — | — |
| `wf` | Manuia | Unverified | — | — | — |
| `ws` | Vodafone, Digicel | Partly verified | Operator report | FY2024-25 | [Amalgamated Telecom Holdings (ATH) Annual Report 2025](https://www.ath.com.fj/wp-content/uploads/2025/10/2025_ATH-ANNUAL-REPORT_HIGH-RES-1.pdf) |
| `xk` | Vala, IPKO | Unverified | — | — | — |
| `ye` | Yemen Mobile, Sabafon, YOU, Y | Unverified | — | — | — |
| `yt` | Orange, SFR, Only | Unverified | — | — | — |
| `za` | Vodacom, MTN, Telkom, Cell C, rain | Partly verified | Operator report | Q2 2026 (Vodacom, Telkom: 30 Jun 2026; M | [Vodacom Group trading update for the quarter ended 30 June 2026 (1Q27); MTN Group FY2025 a](https://www.vodacom.com/pdf/investor/quarterly-results/2026/vodacom-trading-update-1q27.pdf) |
| `zm` | Airtel, MTN, Zamtel, Zed Mobile | Partly verified | Secondary | 2023 (shares); Q4 2025 (MTN subscribers) | [ITWeb Africa, 12 Mar 2024: 'Zambia mobile sector exceeds $389 million' (quoting ZICTA 2023](https://itweb.africa/content/KWEBbvyLzKxqmRjO) |
| `zw` | Econet, NetOne, Telecel | Verified | Regulator | Q4 2025 | [POTRAZ Abridged Postal & Telecommunications Sector Performance Report, Fourth Quarter 2025](https://t3n9sm.c2.acecdn.net/wp-content/uploads/2026/04/Q4-2025-Abridged-Sector-performance-report-HMed-final.pdf) |
