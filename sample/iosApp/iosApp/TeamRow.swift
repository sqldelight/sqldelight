//
//  TeamRow.swift
//  iosApp
//
//  Created by Kevin Galligan on 1/18/19.
//  Copyright © 2019 Kevin Galligan. All rights reserved.
//

import Foundation
import UIKit
import common

class TeamRow: UITableViewCell, TeamCell {
    @IBOutlet weak var nameText: UILabel!
    @IBOutlet weak var coachText: UILabel!
    @IBOutlet weak var foundedText: UILabel!
    
    func fillName(name: String) {
        nameText?.text = name
    }
    
    func fillCoach(coach: String) {
        coachText.text = coach
    }
    
    func fillFounded(founded: String) {
        foundedText.text = founded
    }
}
