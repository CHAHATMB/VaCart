import unittest
from bs4 import BeautifulSoup

class NTESStationParser:
    @staticmethod
    def parse_station_html(html_snippet):
        soup = BeautifulSoup(html_snippet, 'html.parser')
        
        # 1. Live location check (green_dot_blink.gif)
        has_green_dot = bool(soup.find('img', src=lambda s: s and 'green_dot_blink' in s))
        
        # 2. Updated On timestamp
        updated_on = ""
        for font in soup.find_all(['font', 'span']):
            if 'Updated on' in font.text:
                parent_div = font.find_parent('div')
                b = parent_div.find('b') if parent_div else font.find('b')
                if b:
                    updated_on = b.text.strip()
                    break
        
        # 3. Live Event Status Text (e.g. Departed from ANKAI (ANK) on 30-Aug-2026 22:09)
        live_status_text = ""
        green_font = soup.find('font', color=lambda c: c and 'green' in c.lower())
        if green_font:
            live_status_text = green_font.text.strip()
            
        # 4. Stop Type
        is_non_stopping = 'Non-Stopping' in soup.text
        
        # 5. Station Name, Code, Distance & Platform
        station_name = ""
        station_code = ""
        distance = ""
        platform = ""
        
        center_div = soup.find('div', style=lambda s: s and 'flex:1' in s)
        if center_div:
            # Check platform badge: e.g. <span class="w3-round w3-orange">PF 2</span>
            pf_span = center_div.find('span', class_=lambda c: c and 'w3-orange' in c)
            if pf_span:
                platform = pf_span.text.strip()
                
            b_tags = center_div.find_all('b')
            if b_tags:
                first_b_text = b_tags[0].text.strip()
                if '-' in first_b_text and not 'KMs' in first_b_text:
                    parts = first_b_text.split('-')
                    station_name = parts[0].strip()
                    station_code = parts[1].strip()
                else:
                    station_name = first_b_text
                    
            if not station_code and len(b_tags) > 1:
                code_copy = BeautifulSoup(str(b_tags[1]), 'html.parser')
                for child in code_copy.find_all('span'):
                    child.decompose()
                code_text = code_copy.text.strip()
                if code_text and not 'KMs' in code_text:
                    station_code = code_text.split()[0].strip()
                    
            # Distance (e.g., <b>721</b> KMs or <b>683</b> KMs)
            for b in b_tags:
                parent_text = b.parent.text if b.parent else ""
                if 'KMs' in parent_text and b.text.strip().isdigit():
                    distance = f"{b.text.strip()} KMs"
                    break
                    
        # 6. Scheduled & Actual Arrival Timings & Delays (from Left Container)
        sch_arr = ""
        act_arr = ""
        arr_delay = ""
        left_div = soup.find('div', style=lambda s: s and 'float:left' in s and ('100px' in s or 'padding-left' in s))
        if left_div:
            fonts = left_div.find_all('font')
            if len(fonts) >= 1:
                sch_b = fonts[0].find('b')
                sch_arr = sch_b.text.strip() if sch_b else fonts[0].text.strip()
            if len(fonts) >= 2:
                act_b = fonts[1].find('b')
                act_arr = act_b.text.strip() if act_b else ""
                delay_span = fonts[1].find('span', class_=lambda c: c and 'w3-round' in c)
                if delay_span:
                    arr_delay = delay_span.text.strip()

        # 7. Scheduled & Actual Departure Timings & Delays (from Right Container)
        sch_dep = ""
        act_dep = ""
        dep_delay = ""
        right_divs = soup.find_all('div', style=lambda s: s and 'float:right' in s and 'text-align:right' in s)
        for rd in right_divs:
            fonts = rd.find_all('font')
            if fonts and any(':' in f.text for f in fonts):
                if len(fonts) >= 1:
                    sch_b = fonts[0].find('b')
                    sch_dep = sch_b.text.strip() if sch_b else fonts[0].text.strip()
                if len(fonts) >= 2:
                    act_b = fonts[1].find('b')
                    act_dep = act_b.text.strip() if act_b else ""
                    delay_span = fonts[1].find('span', class_=lambda c: c and 'w3-round' in c)
                    if delay_span:
                        dep_delay = delay_span.text.strip()

        # 8. Coach Position Modal details
        coach_positions = []
        divyangjan_info = ""
        modal = soup.find('div', class_='modal')
        if modal:
            coach_divs = modal.find_all('div', style=lambda s: s and '45px' in s and '60px' in s)
            for cd in coach_divs:
                divs = cd.find_all('div')
                if len(divs) >= 3:
                    c_type = divs[0].text.strip()
                    c_name = divs[1].find('b').text.strip() if divs[1].find('b') else divs[1].text.strip()
                    c_pos = divs[2].text.strip()
                    coach_positions.append({
                        'coach_type': c_type,
                        'coach_name': c_name,
                        'position_index': c_pos
                    })
            div_font = modal.find('font', color='red')
            if div_font:
                divyangjan_info = div_font.text.strip()

        return {
            'station_name': station_name,
            'station_code': station_code if station_code else station_name,
            'distance': distance,
            'platform': platform,
            'is_non_stopping': is_non_stopping,
            'is_live_location': has_green_dot or bool(live_status_text),
            'updated_on': updated_on,
            'live_status_text': live_status_text,
            'sch_arrival': sch_arr,
            'act_arrival': act_arr,
            'arr_delay': arr_delay,
            'sch_departure': sch_dep,
            'act_departure': act_dep,
            'dep_delay': dep_delay,
            'coach_positions': coach_positions,
            'divyangjan_info': divyangjan_info
        }

class TestNTESParser(unittest.TestCase):
    def test_live_location_station(self):
        html = """
        <div class="w3-container" style="float:left;width:100px;text-align:right;padding-top: 8px">
        <font size="1" style="font-weight: normal;">
          <span>Updated on</span><br>
          <span><b>30-Aug-2026 22:11</b></span>
          </font>
        </div>
        <div class="w3-bar-block w3-border" style="width:12px;display:block;background-image:url('images/track_gray.png')">
        <div class="w3-bar-item" style="height:auto;margin-left:-23px;min-height:40px;"><img alt="NTES" src="images/green_dot_blink.gif" height="24px" width="24px"></div>
        </div>
        <div class="w3-container" style="float:right;flex:1;padding-left:0;padding-right:0;display:flex;padding-top:2px;padding-bottom:4px;">
        <div class="w3-container" style="flex:1;">
        <span><font style="font-size:10px;"><b>ANKAI - ANK</b><br><b>721</b> KMs</font></span>
        <div style="display: flex;justify-content: flex-start;">
        <span><img alt="NTES" src="images/green_dot_blink.gif" height="14px" width="14px" style="margin-top: -4px;">
        <font size="1" color="GREEN"><b>Departed from ANKAI&nbsp;(ANK) on 30-Aug-2026 22:09</b></font></span>
        </div></div>
        <div class="w3-container" style="float:right;text-align:right;">
        <font color="red" style="font-size:9px;"><span><b>Non-Stopping</b></span></font>
        </div></div>
        """
        data = NTESStationParser.parse_station_html(html)
        self.assertTrue(data['is_live_location'])
        self.assertEqual(data['station_name'], 'ANKAI')
        self.assertEqual(data['station_code'], 'ANK')
        self.assertEqual(data['distance'], '721 KMs')
        self.assertEqual(data['updated_on'], '30-Aug-2026 22:11')
        self.assertIn('Departed from ANKAI', data['live_status_text'])
        self.assertTrue(data['is_non_stopping'])

    def test_normal_stopping_station(self):
        html = """
        <div class="w3-container" style="float:left;width:100px;text-align:right;padding-left: 10px;">
              <span><b><font size="1">20:04 30-Aug</font></b></span><br>
              <span><font size="1" color="red">
                <b>21:19 30-Aug</b>
                <br><span class="w3-round w3-red" style="padding: 1px 4px;">01:15 Hr</span>
                </font></span>
        </div>
        <div class="w3-container" style="float:right;flex:1;padding-left:0px;padding-right:0px;display:flex;">
             <div class="w3-container" style="float:left;flex:1;">
              <span><font size="1"><b>NANDGAON</b><br>
              <div class="w3-container" style="flex:1;padding:0px;display:inline-block;width:100%;text-align: center;">
              <div style="float:left;padding: 0px;"><b>NGN  <span class="w3-round w3-orange" style="padding: 1px 4px;">PF 2</span></b></div>
            </div>
            <br><b>683</b> KMs</font></span>
            </div>
            <div class="w3-container" style="float:right;text-align:right;width:100px;padding-right:10px;padding-left:10px;">
              <span><b><font size="1">20:05 30-Aug</font></b></span><br>
              <span><font size="1" color="red">
                <b>21:20 30-Aug</b>
                <br><span class="w3-round w3-red" style="padding: 1px 4px;">01:15 Hr</span>
                </font></span>
            </div>
        </div>
        """
        data = NTESStationParser.parse_station_html(html)
        self.assertEqual(data['station_name'], 'NANDGAON')
        self.assertEqual(data['station_code'], 'NGN')
        self.assertEqual(data['platform'], 'PF 2')
        self.assertEqual(data['distance'], '683 KMs')
        self.assertEqual(data['sch_arrival'], '20:04 30-Aug')
        self.assertEqual(data['act_arrival'], '21:19 30-Aug')
        self.assertEqual(data['arr_delay'], '01:15 Hr')
        self.assertEqual(data['sch_departure'], '20:05 30-Aug')
        self.assertEqual(data['act_departure'], '21:20 30-Aug')
        self.assertEqual(data['dep_delay'], '01:15 Hr')

if __name__ == '__main__':
    unittest.main()
