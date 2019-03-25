//
//  TeamRow.swift
//  iosApp
//
//  Copyright © 2019 Square, Inc.. All rights reserved.
//

import Foundation
import UIKit
import main

class TeamRow: UITableViewCell {
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
