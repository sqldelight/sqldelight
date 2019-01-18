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
    func fillName(name: String) {
        textLabel?.text = name
    }
    
    func fillCoach(coach: String) {
        detailTextLabel?.text = coach
    }
    
    func fillFounded(founded: String) {
        print(founded)
    }
}
